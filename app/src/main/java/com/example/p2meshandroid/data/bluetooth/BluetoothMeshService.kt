package com.example.p2meshandroid.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Service UUID for P2P Mesh network discovery
 */
val MESH_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
val MESH_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")

/**
 * Represents a discovered Bluetooth peer
 */
data class BluetoothPeer(
    val address: String,
    val name: String?,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis(),
    val isConnected: Boolean = false
)

/**
 * State of Bluetooth operations
 */
sealed class BluetoothState {
    object Disabled : BluetoothState()
    object Enabling : BluetoothState()
    object Idle : BluetoothState()
    object Scanning : BluetoothState()
    object Advertising : BluetoothState()
    object ScanningAndAdvertising : BluetoothState()
    data class Error(val message: String) : BluetoothState()
}

/**
 * Bluetooth LE Mesh Service for P2P communication
 */
@SuppressLint("MissingPermission")
class BluetoothMeshService(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bleScanner: BluetoothLeScanner? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null

    private val _state = MutableStateFlow<BluetoothState>(BluetoothState.Idle)
    val state: StateFlow<BluetoothState> = _state.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<BluetoothPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<BluetoothPeer>> = _discoveredPeers.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<BluetoothPeer>>(emptyList())
    val connectedPeers: StateFlow<List<BluetoothPeer>> = _connectedPeers.asStateFlow()

    private val peersMap = ConcurrentHashMap<String, BluetoothPeer>()
    private val connectedDevices = ConcurrentHashMap<String, BluetoothGatt>()

    private var isScanning = false
    private var isAdvertising = false

    // Callback for receiving data from peers
    var onDataReceived: ((address: String, data: ByteArray) -> Unit)? = null

    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Start scanning for nearby mesh peers
     */
    fun startScanning(): Result<Unit> {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _state.value = BluetoothState.Disabled
            return Result.failure(Exception("Bluetooth is not available or disabled"))
        }

        if (isScanning) {
            return Result.success(Unit)
        }

        return try {
            bleScanner = bluetoothAdapter.bluetoothLeScanner
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            val scanFilters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
                    .build()
            )

            bleScanner?.startScan(scanFilters, scanSettings, scanCallback)
            isScanning = true
            updateState()
            Log.d(TAG, "BLE scanning started")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scanning", e)
            _state.value = BluetoothState.Error("Failed to start scanning: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Stop scanning
     */
    fun stopScanning() {
        if (isScanning) {
            try {
                bleScanner?.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan", e)
            }
            isScanning = false
            updateState()
            Log.d(TAG, "BLE scanning stopped")
        }
    }

    /**
     * Start advertising as a mesh peer
     */
    fun startAdvertising(): Result<Unit> {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _state.value = BluetoothState.Disabled
            return Result.failure(Exception("Bluetooth is not available or disabled"))
        }

        if (isAdvertising) {
            return Result.success(Unit)
        }

        return try {
            bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
            if (bleAdvertiser == null) {
                return Result.failure(Exception("BLE advertising not supported"))
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
                .build()

            val scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            // Start GATT server for incoming connections
            startGattServer()

            bleAdvertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
            isAdvertising = true
            updateState()
            Log.d(TAG, "BLE advertising started")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start advertising", e)
            _state.value = BluetoothState.Error("Failed to start advertising: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Stop advertising
     */
    fun stopAdvertising() {
        if (isAdvertising) {
            try {
                bleAdvertiser?.stopAdvertising(advertiseCallback)
                gattServer?.close()
                gattServer = null
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping advertising", e)
            }
            isAdvertising = false
            updateState()
            Log.d(TAG, "BLE advertising stopped")
        }
    }

    /**
     * Start both scanning and advertising (mesh mode)
     */
    fun startMeshMode(): Result<Unit> {
        val scanResult = startScanning()
        val advertiseResult = startAdvertising()

        return if (scanResult.isSuccess && advertiseResult.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(
                Exception(
                    "Failed to start mesh mode: " +
                            "Scan=${scanResult.exceptionOrNull()?.message}, " +
                            "Advertise=${advertiseResult.exceptionOrNull()?.message}"
                )
            )
        }
    }

    /**
     * Stop mesh mode (both scanning and advertising)
     */
    fun stopMeshMode() {
        stopScanning()
        stopAdvertising()
        disconnectAll()
    }

    /**
     * Connect to a discovered peer
     */
    fun connectToPeer(address: String): Result<Unit> {
        if (connectedDevices.containsKey(address)) {
            return Result.success(Unit)
        }

        return try {
            val device = bluetoothAdapter?.getRemoteDevice(address)
                ?: return Result.failure(Exception("Device not found"))

            val gatt = device.connectGatt(context, false, gattCallback)
            connectedDevices[address] = gatt
            Log.d(TAG, "Connecting to peer: $address")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to peer", e)
            Result.failure(e)
        }
    }

    /**
     * Disconnect from a peer
     */
    fun disconnectFromPeer(address: String) {
        connectedDevices.remove(address)?.let { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from peer", e)
            }
        }
        updateConnectedPeers(address, false)
        Log.d(TAG, "Disconnected from peer: $address")
    }

    /**
     * Disconnect from all peers
     */
    private fun disconnectAll() {
        connectedDevices.forEach { (address, gatt) ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from $address", e)
            }
        }
        connectedDevices.clear()
        _connectedPeers.value = emptyList()
    }

    /**
     * Send data to a connected peer
     */
    fun sendData(address: String, data: ByteArray): Result<Unit> {
        val gatt = connectedDevices[address]
            ?: return Result.failure(Exception("Peer not connected"))

        return try {
            val service = gatt.getService(MESH_SERVICE_UUID)
                ?: return Result.failure(Exception("Mesh service not found"))

            val characteristic = service.getCharacteristic(MESH_CHARACTERISTIC_UUID)
                ?: return Result.failure(Exception("Mesh characteristic not found"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = data
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data", e)
            Result.failure(e)
        }
    }

    /**
     * Send data to all connected peers (broadcast)
     */
    fun broadcastData(data: ByteArray): Result<Int> {
        var successCount = 0
        connectedDevices.keys.forEach { address ->
            if (sendData(address, data).isSuccess) {
                successCount++
            }
        }
        return Result.success(successCount)
    }

    /**
     * Get list of discovered peers
     */
    fun getDiscoveredPeers(): List<BluetoothPeer> = _discoveredPeers.value

    /**
     * Get connected peer count
     */
    fun getConnectedPeerCount(): Int = _connectedPeers.value.size

    /**
     * Start GATT server to receive incoming connections
     */
    private fun startGattServer() {
        if (gattServer != null) return

        gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)

        val service = BluetoothGattService(
            MESH_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val characteristic = BluetoothGattCharacteristic(
            MESH_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE or
                    BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
    }

    private fun updateState() {
        _state.value = when {
            !isBluetoothEnabled() -> BluetoothState.Disabled
            isScanning && isAdvertising -> BluetoothState.ScanningAndAdvertising
            isScanning -> BluetoothState.Scanning
            isAdvertising -> BluetoothState.Advertising
            else -> BluetoothState.Idle
        }
    }

    private fun updateConnectedPeers(address: String, isConnected: Boolean) {
        val peer = peersMap[address]?.copy(isConnected = isConnected)
        if (peer != null) {
            peersMap[address] = peer
        }
        _connectedPeers.value = peersMap.values.filter { it.isConnected }.toList()
    }

    // Scan callback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val peer = BluetoothPeer(
                address = device.address,
                name = device.name,
                rssi = result.rssi,
                lastSeen = System.currentTimeMillis()
            )
            peersMap[device.address] = peer
            _discoveredPeers.value = peersMap.values.toList()
            Log.d(TAG, "Discovered peer: ${device.address}, RSSI: ${result.rssi}")
        }

        override fun onScanFailed(errorCode: Int) {
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                else -> "Unknown error: $errorCode"
            }
            Log.e(TAG, "Scan failed: $errorMsg")
            _state.value = BluetoothState.Error("Scan failed: $errorMsg")
            isScanning = false
        }
    }

    // Advertise callback
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            val errorMsg = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                else -> "Unknown error: $errorCode"
            }
            Log.e(TAG, "Advertising failed: $errorMsg")
            _state.value = BluetoothState.Error("Advertising failed: $errorMsg")
            isAdvertising = false
        }
    }

    // GATT client callback (for outgoing connections)
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server: $address")
                    gatt.discoverServices()
                    updateConnectedPeers(address, true)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server: $address")
                    connectedDevices.remove(address)
                    gatt.close()
                    updateConnectedPeers(address, false)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered for ${gatt.device.address}")
                // Enable notifications for the mesh characteristic
                val service = gatt.getService(MESH_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(MESH_CHARACTERISTIC_UUID)
                characteristic?.let {
                    gatt.setCharacteristicNotification(it, true)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == MESH_CHARACTERISTIC_UUID) {
                Log.d(TAG, "Received data from ${gatt.device.address}: ${value.size} bytes")
                onDataReceived?.invoke(gatt.device.address, value)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == MESH_CHARACTERISTIC_UUID) {
                @Suppress("DEPRECATION")
                val value = characteristic.value
                Log.d(TAG, "Received data from ${gatt.device.address}: ${value?.size} bytes")
                value?.let { onDataReceived?.invoke(gatt.device.address, it) }
            }
        }
    }

    // GATT server callback (for incoming connections)
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            val address = device.address
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Device connected: $address")
                    val peer = BluetoothPeer(
                        address = address,
                        name = device.name,
                        rssi = 0,
                        isConnected = true
                    )
                    peersMap[address] = peer
                    _connectedPeers.value = peersMap.values.filter { it.isConnected }.toList()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Device disconnected: $address")
                    updateConnectedPeers(address, false)
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid == MESH_CHARACTERISTIC_UUID) {
                Log.d(TAG, "Received write from ${device.address}: ${value.size} bytes")
                onDataReceived?.invoke(device.address, value)

                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                    )
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == MESH_CHARACTERISTIC_UUID) {
                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    ByteArray(0)
                )
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothMeshService"
    }
}
