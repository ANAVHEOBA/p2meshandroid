package com.example.p2meshandroid.data.repository

import android.content.Context
import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import com.example.p2meshandroid.data.wifidirect.WiFiDirectMeshService
import com.example.p2meshandroid.data.wifidirect.WiFiDirectPeer
import com.example.p2meshandroid.data.wifidirect.WiFiDirectState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository for WiFi Direct mesh operations.
 * Bridges the WiFiDirectMeshService with the rest of the app.
 */
class WiFiDirectRepository(
    private val context: Context,
    private val meshRepository: MeshRepository
) {
    private var wifiDirectService: WiFiDirectMeshService? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: StateFlow<WiFiDirectState>
        get() = getOrCreateService().state

    val discoveredPeers: StateFlow<List<WiFiDirectPeer>>
        get() = getOrCreateService().discoveredPeers

    val connectionInfo: StateFlow<WifiP2pInfo?>
        get() = getOrCreateService().connectionInfo

    private fun getOrCreateService(): WiFiDirectMeshService {
        if (wifiDirectService == null) {
            wifiDirectService = WiFiDirectMeshService(context).apply {
                // Set up data received callback to integrate with mesh
                onDataReceived = { address, data ->
                    handleReceivedData(address, data)
                }
            }
        }
        return wifiDirectService!!
    }

    /**
     * Initialize WiFi Direct
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Main) {
        getOrCreateService().initialize()
    }

    /**
     * Check if WiFi Direct is available
     */
    fun isAvailable(): Boolean = getOrCreateService().isAvailable()

    /**
     * Start peer discovery
     */
    suspend fun startDiscovery(): Result<Unit> = withContext(Dispatchers.Main) {
        getOrCreateService().startDiscovery()
    }

    /**
     * Stop peer discovery
     */
    suspend fun stopDiscovery(): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            getOrCreateService().stopDiscovery()
        }
    }

    /**
     * Connect to a peer
     */
    suspend fun connectToPeer(deviceAddress: String): Result<Unit> = withContext(Dispatchers.Main) {
        getOrCreateService().connectToPeer(deviceAddress)
    }

    /**
     * Disconnect from current group
     */
    suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.Main) {
        getOrCreateService().disconnect()
    }

    /**
     * Create a WiFi Direct group (become group owner)
     */
    suspend fun createGroup(): Result<Unit> = withContext(Dispatchers.Main) {
        getOrCreateService().createGroup()
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
     * Sync mesh state with connected peers via WiFi Direct
     */
    suspend fun syncWithPeers(): Result<MergeStats> = withContext(Dispatchers.IO) {
        runCatching {
            // Get our local mesh state
            val localState = meshRepository.getLocalState().getOrThrow()

            // Broadcast to all connected peers
            val sentCount = getOrCreateService().broadcastData(localState).getOrThrow()
            Log.d(TAG, "Sent mesh state to $sentCount peers")

            // Return merge stats (actual merge happens when we receive peer's state)
            MergeStats(newEntries = 0UL, totalEntries = 0UL)
        }
    }

    /**
     * Handle data received from a WiFi Direct peer
     */
    private fun handleReceivedData(address: String, data: ByteArray) {
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
     * Get connected peer count
     */
    fun getConnectedPeerCount(): Int = getOrCreateService().getConnectedPeerCount()

    /**
     * Cleanup resources
     */
    fun cleanup() {
        wifiDirectService?.cleanup()
        wifiDirectService = null
    }

    companion object {
        private const val TAG = "WiFiDirectRepository"
    }
}
