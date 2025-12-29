package com.example.p2meshandroid.data.repository

import android.content.Context
import android.util.Log
import com.example.p2meshandroid.data.bluetooth.BluetoothMeshService
import com.example.p2meshandroid.data.bluetooth.BluetoothPeer
import com.example.p2meshandroid.data.bluetooth.BluetoothState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository for Bluetooth mesh operations.
 * Bridges the BluetoothMeshService with the rest of the app.
 */
class BluetoothRepository(
    private val context: Context,
    private val meshRepository: MeshRepository
) {
    private var bluetoothService: BluetoothMeshService? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: StateFlow<BluetoothState>
        get() = getOrCreateService().state

    val discoveredPeers: StateFlow<List<BluetoothPeer>>
        get() = getOrCreateService().discoveredPeers

    val connectedPeers: StateFlow<List<BluetoothPeer>>
        get() = getOrCreateService().connectedPeers

    private fun getOrCreateService(): BluetoothMeshService {
        if (bluetoothService == null) {
            bluetoothService = BluetoothMeshService(context).apply {
                // Set up data received callback to integrate with mesh
                onDataReceived = { address, data ->
                    handleReceivedData(address, data)
                }
            }
        }
        return bluetoothService!!
    }

    /**
     * Check if Bluetooth is available on this device
     */
    fun isBluetoothAvailable(): Boolean = getOrCreateService().isBluetoothAvailable()

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean = getOrCreateService().isBluetoothEnabled()

    /**
     * Start Bluetooth mesh mode (scanning + advertising)
     */
    suspend fun startMeshMode(): Result<Unit> = withContext(Dispatchers.IO) {
        getOrCreateService().startMeshMode()
    }

    /**
     * Stop Bluetooth mesh mode
     */
    suspend fun stopMeshMode(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getOrCreateService().stopMeshMode()
        }
    }

    /**
     * Start scanning for nearby peers
     */
    suspend fun startScanning(): Result<Unit> = withContext(Dispatchers.IO) {
        getOrCreateService().startScanning()
    }

    /**
     * Stop scanning
     */
    suspend fun stopScanning(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getOrCreateService().stopScanning()
        }
    }

    /**
     * Start advertising as a mesh peer
     */
    suspend fun startAdvertising(): Result<Unit> = withContext(Dispatchers.IO) {
        getOrCreateService().startAdvertising()
    }

    /**
     * Stop advertising
     */
    suspend fun stopAdvertising(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getOrCreateService().stopAdvertising()
        }
    }

    /**
     * Connect to a discovered peer
     */
    suspend fun connectToPeer(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        getOrCreateService().connectToPeer(address)
    }

    /**
     * Disconnect from a peer
     */
    suspend fun disconnectFromPeer(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getOrCreateService().disconnectFromPeer(address)
        }
    }

    /**
     * Send data to a specific peer
     */
    suspend fun sendData(address: String, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        getOrCreateService().sendData(address, data)
    }

    /**
     * Broadcast data to all connected peers
     */
    suspend fun broadcastData(data: ByteArray): Result<Int> = withContext(Dispatchers.IO) {
        getOrCreateService().broadcastData(data)
    }

    /**
     * Sync mesh state with a Bluetooth peer
     */
    suspend fun syncWithPeer(address: String): Result<MergeStats> = withContext(Dispatchers.IO) {
        runCatching {
            // Get our local mesh state
            val localState = meshRepository.getLocalState().getOrThrow()

            // Send to peer via Bluetooth
            getOrCreateService().sendData(address, localState).getOrThrow()

            // Return merge stats (actual merge happens when we receive peer's state)
            MergeStats(newEntries = 0UL, totalEntries = 0UL)
        }
    }

    /**
     * Handle data received from a Bluetooth peer
     */
    private fun handleReceivedData(address: String, data: ByteArray) {
        // This is called from the Bluetooth thread, so we need to handle it appropriately
        // The data could be mesh state from another peer
        scope.launch {
            try {
                // Try to merge the received data as mesh state
                val result = meshRepository.mergeRemoteState(data)
                result.onSuccess { stats ->
                    Log.d(TAG, "Merged state from $address: ${stats.newEntries} new entries")
                }
                result.onFailure { error ->
                    Log.e(TAG, "Failed to merge state from $address: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing data from $address", e)
            }
        }
    }

    /**
     * Get discovered peer count
     */
    fun getDiscoveredPeerCount(): Int = getOrCreateService().getDiscoveredPeers().size

    /**
     * Get connected peer count
     */
    fun getConnectedPeerCount(): Int = getOrCreateService().getConnectedPeerCount()

    companion object {
        private const val TAG = "BluetoothRepository"
    }
}
