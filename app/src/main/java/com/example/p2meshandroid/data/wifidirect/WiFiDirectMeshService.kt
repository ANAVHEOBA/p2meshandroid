package com.example.p2meshandroid.data.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.os.Build
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a discovered WiFi Direct peer
 */
data class WiFiDirectPeer(
    val deviceAddress: String,
    val deviceName: String,
    val status: Int,
    val isGroupOwner: Boolean = false,
    val isConnected: Boolean = false
) {
    val statusText: String
        get() = when (status) {
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
}

/**
 * State of WiFi Direct operations
 */
sealed class WiFiDirectState {
    object Disabled : WiFiDirectState()
    object Idle : WiFiDirectState()
    object Discovering : WiFiDirectState()
    object Connecting : WiFiDirectState()
    object Connected : WiFiDirectState()
    data class GroupFormed(val isOwner: Boolean, val ownerAddress: String?) : WiFiDirectState()
    data class Error(val message: String) : WiFiDirectState()
}

/**
 * WiFi Direct Mesh Service for P2P communication
 */
@SuppressLint("MissingPermission")
class WiFiDirectMeshService(private val context: Context) {

    private val wifiP2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager

    private var channel: WifiP2pManager.Channel? = null
    private var receiver: WiFiDirectBroadcastReceiver? = null
    private var serverSocket: ServerSocket? = null
    private var isRegistered = false

    private val _state = MutableStateFlow<WiFiDirectState>(WiFiDirectState.Idle)
    val state: StateFlow<WiFiDirectState> = _state.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<WiFiDirectPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<WiFiDirectPeer>> = _discoveredPeers.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private val connectedSockets = ConcurrentHashMap<String, Socket>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isDiscovering = false
    private var serverJob: Job? = null

    // Callback for receiving data from peers
    var onDataReceived: ((address: String, data: ByteArray) -> Unit)? = null

    companion object {
        private const val TAG = "WiFiDirectMeshService"
        private const val SERVER_PORT = 8888
        private const val SOCKET_TIMEOUT = 5000
    }

    /**
     * Initialize WiFi Direct
     */
    fun initialize(): Result<Unit> {
        if (wifiP2pManager == null) {
            _state.value = WiFiDirectState.Disabled
            return Result.failure(Exception("WiFi Direct not supported"))
        }

        return try {
            channel = wifiP2pManager.initialize(context, Looper.getMainLooper()) {
                Log.d(TAG, "WiFi P2P channel disconnected")
                _state.value = WiFiDirectState.Disabled
            }

            registerReceiver()
            _state.value = WiFiDirectState.Idle
            Log.d(TAG, "WiFi Direct initialized")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WiFi Direct", e)
            _state.value = WiFiDirectState.Error("Initialization failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Check if WiFi Direct is available
     */
    fun isAvailable(): Boolean = wifiP2pManager != null && channel != null

    /**
     * Start peer discovery
     */
    fun startDiscovery(): Result<Unit> {
        val manager = wifiP2pManager ?: return Result.failure(Exception("WiFi Direct not available"))
        val ch = channel ?: return Result.failure(Exception("Channel not initialized"))

        if (isDiscovering) {
            return Result.success(Unit)
        }

        return try {
            manager.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    isDiscovering = true
                    _state.value = WiFiDirectState.Discovering
                    Log.d(TAG, "Peer discovery started")
                }

                override fun onFailure(reason: Int) {
                    val errorMsg = getErrorMessage(reason)
                    Log.e(TAG, "Peer discovery failed: $errorMsg")
                    _state.value = WiFiDirectState.Error("Discovery failed: $errorMsg")
                }
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            Result.failure(e)
        }
    }

    /**
     * Stop peer discovery
     */
    fun stopDiscovery() {
        val manager = wifiP2pManager ?: return
        val ch = channel ?: return

        if (!isDiscovering) return

        try {
            manager.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    isDiscovering = false
                    if (_state.value is WiFiDirectState.Discovering) {
                        _state.value = WiFiDirectState.Idle
                    }
                    Log.d(TAG, "Peer discovery stopped")
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "Failed to stop discovery: ${getErrorMessage(reason)}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }
    }

    /**
     * Connect to a peer
     */
    fun connectToPeer(deviceAddress: String): Result<Unit> {
        val manager = wifiP2pManager ?: return Result.failure(Exception("WiFi Direct not available"))
        val ch = channel ?: return Result.failure(Exception("Channel not initialized"))

        return try {
            val config = WifiP2pConfig().apply {
                this.deviceAddress = deviceAddress
                this.wps.setup = WpsInfo.PBC
            }

            _state.value = WiFiDirectState.Connecting

            manager.connect(ch, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Connection initiated to $deviceAddress")
                }

                override fun onFailure(reason: Int) {
                    val errorMsg = getErrorMessage(reason)
                    Log.e(TAG, "Connection failed: $errorMsg")
                    _state.value = WiFiDirectState.Error("Connection failed: $errorMsg")
                }
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect", e)
            _state.value = WiFiDirectState.Error("Connection error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Disconnect from current group
     */
    fun disconnect(): Result<Unit> {
        val manager = wifiP2pManager ?: return Result.failure(Exception("WiFi Direct not available"))
        val ch = channel ?: return Result.failure(Exception("Channel not initialized"))

        return try {
            // Close all sockets
            connectedSockets.values.forEach { socket ->
                try {
                    socket.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing socket", e)
                }
            }
            connectedSockets.clear()

            // Stop server
            stopServer()

            manager.removeGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    _state.value = WiFiDirectState.Idle
                    _connectionInfo.value = null
                    Log.d(TAG, "Disconnected from group")
                }

                override fun onFailure(reason: Int) {
                    Log.e(TAG, "Failed to disconnect: ${getErrorMessage(reason)}")
                }
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
            Result.failure(e)
        }
    }

    /**
     * Create a WiFi Direct group (become group owner)
     */
    fun createGroup(): Result<Unit> {
        val manager = wifiP2pManager ?: return Result.failure(Exception("WiFi Direct not available"))
        val ch = channel ?: return Result.failure(Exception("Channel not initialized"))

        return try {
            manager.createGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Group created successfully")
                }

                override fun onFailure(reason: Int) {
                    val errorMsg = getErrorMessage(reason)
                    Log.e(TAG, "Failed to create group: $errorMsg")
                    _state.value = WiFiDirectState.Error("Group creation failed: $errorMsg")
                }
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group", e)
            Result.failure(e)
        }
    }

    /**
     * Start server socket to receive connections (for group owner)
     */
    fun startServer() {
        if (serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                serverSocket?.reuseAddress = true
                Log.d(TAG, "Server started on port $SERVER_PORT")

                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        Log.d(TAG, "Client connected: ${client.inetAddress.hostAddress}")

                        launch {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Error accepting client", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }
    }

    /**
     * Stop server socket
     */
    private fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serverSocket = null
    }

    /**
     * Handle incoming client connection
     */
    private suspend fun handleClient(socket: Socket) {
        val address = socket.inetAddress.hostAddress ?: return
        connectedSockets[address] = socket

        try {
            val inputStream = socket.getInputStream()
            val buffer = ByteArray(4096)

            while (socket.isConnected && !socket.isClosed) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                val data = buffer.copyOf(bytesRead)
                Log.d(TAG, "Received ${data.size} bytes from $address")
                withContext(Dispatchers.Main) {
                    onDataReceived?.invoke(address, data)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client $address", e)
        } finally {
            connectedSockets.remove(address)
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing client socket", e)
            }
        }
    }

    /**
     * Connect to group owner (for clients)
     */
    fun connectToGroupOwner(ownerAddress: String): Result<Socket> {
        return try {
            val socket = Socket()
            socket.bind(null)
            socket.connect(InetSocketAddress(ownerAddress, SERVER_PORT), SOCKET_TIMEOUT)
            connectedSockets[ownerAddress] = socket

            // Start receiving data
            scope.launch {
                handleClient(socket)
            }

            Log.d(TAG, "Connected to group owner: $ownerAddress")
            Result.success(socket)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to group owner", e)
            Result.failure(e)
        }
    }

    /**
     * Send data to a specific peer
     */
    fun sendData(address: String, data: ByteArray): Result<Unit> {
        val socket = connectedSockets[address]
            ?: return Result.failure(Exception("Not connected to $address"))

        return try {
            val outputStream = socket.getOutputStream()
            outputStream.write(data)
            outputStream.flush()
            Log.d(TAG, "Sent ${data.size} bytes to $address")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data to $address", e)
            connectedSockets.remove(address)
            Result.failure(e)
        }
    }

    /**
     * Broadcast data to all connected peers
     */
    fun broadcastData(data: ByteArray): Result<Int> {
        var successCount = 0
        connectedSockets.keys.forEach { address ->
            if (sendData(address, data).isSuccess) {
                successCount++
            }
        }
        return Result.success(successCount)
    }

    /**
     * Get connected peer count
     */
    fun getConnectedPeerCount(): Int = connectedSockets.size

    /**
     * Register broadcast receiver
     */
    private fun registerReceiver() {
        if (isRegistered) return

        receiver = WiFiDirectBroadcastReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }
        isRegistered = true
    }

    /**
     * Unregister broadcast receiver and cleanup
     */
    fun cleanup() {
        stopDiscovery()
        disconnect()
        stopServer()

        if (isRegistered && receiver != null) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
            isRegistered = false
        }

        scope.cancel()
        channel?.close()
        channel = null
    }

    /**
     * Get error message for WiFi P2P failure reason
     */
    private fun getErrorMessage(reason: Int): String {
        return when (reason) {
            WifiP2pManager.P2P_UNSUPPORTED -> "P2P not supported"
            WifiP2pManager.BUSY -> "System busy"
            WifiP2pManager.ERROR -> "Internal error"
            else -> "Unknown error ($reason)"
        }
    }

    /**
     * Broadcast receiver for WiFi P2P events
     */
    private inner class WiFiDirectBroadcastReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        Log.d(TAG, "WiFi P2P enabled")
                        if (_state.value is WiFiDirectState.Disabled) {
                            _state.value = WiFiDirectState.Idle
                        }
                    } else {
                        Log.d(TAG, "WiFi P2P disabled")
                        _state.value = WiFiDirectState.Disabled
                    }
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    wifiP2pManager?.requestPeers(channel) { peerList ->
                        val peers = peerList.deviceList.map { device ->
                            WiFiDirectPeer(
                                deviceAddress = device.deviceAddress,
                                deviceName = device.deviceName,
                                status = device.status,
                                isGroupOwner = device.isGroupOwner
                            )
                        }
                        _discoveredPeers.value = peers
                        Log.d(TAG, "Peers updated: ${peers.size} found")
                    }
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    }

                    if (networkInfo?.isConnected == true) {
                        wifiP2pManager?.requestConnectionInfo(channel) { info ->
                            _connectionInfo.value = info
                            if (info.groupFormed) {
                                _state.value = WiFiDirectState.GroupFormed(
                                    isOwner = info.isGroupOwner,
                                    ownerAddress = info.groupOwnerAddress?.hostAddress
                                )
                                Log.d(TAG, "Group formed. Owner: ${info.isGroupOwner}, Address: ${info.groupOwnerAddress?.hostAddress}")

                                // Start server if group owner, or connect to owner if client
                                if (info.isGroupOwner) {
                                    startServer()
                                } else {
                                    info.groupOwnerAddress?.hostAddress?.let { ownerAddr ->
                                        scope.launch {
                                            delay(1000) // Small delay for connection to stabilize
                                            connectToGroupOwner(ownerAddr)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        _connectionInfo.value = null
                        if (_state.value is WiFiDirectState.GroupFormed || _state.value is WiFiDirectState.Connected) {
                            _state.value = WiFiDirectState.Idle
                        }
                        Log.d(TAG, "Disconnected from group")
                    }
                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                    Log.d(TAG, "This device: ${device?.deviceName} (${device?.deviceAddress})")
                }
            }
        }
    }
}
