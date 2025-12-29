package com.example.p2meshandroid.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.p2pmesh_bridge.*

/**
 * Repository for P2P mesh networking operations.
 * Handles state sync between nodes and transport connections.
 */
class MeshRepository(
    private val walletRepository: WalletRepository
) {
    private var meshNode: MeshNode? = null
    private var transport: Transport? = null

    /**
     * Initialize mesh node with current wallet
     */
    suspend fun initializeMeshNode(wallet: Wallet): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            meshNode = MeshNode(wallet)
        }
    }

    // ========== Transport Operations ==========

    /**
     * Start TCP transport on specified address
     */
    suspend fun startTransport(bindAddress: String = "0.0.0.0:0"): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            transport = createTcpTransport(bindAddress)
            transport!!.bindAddress()
        }
    }

    /**
     * Stop transport and disconnect all peers
     */
    suspend fun stopTransport(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            transport?.let { t ->
                // Disconnect all peers
                t.connectedPeers().forEach { peer ->
                    try { t.disconnect(peer.address) } catch (_: Exception) {}
                }
            }
            transport = null
        }
    }

    /**
     * Connect to a peer
     */
    suspend fun connectToPeer(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val t = transport ?: throw IllegalStateException("Transport not started")
            t.connect(address)
        }
    }

    /**
     * Disconnect from a peer
     */
    suspend fun disconnectFromPeer(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val t = transport ?: throw IllegalStateException("Transport not started")
            t.disconnect(address)
        }
    }

    /**
     * Get connected peers
     */
    suspend fun getConnectedPeers(): Result<List<PeerDetails>> = withContext(Dispatchers.IO) {
        runCatching {
            val t = transport ?: return@runCatching emptyList()
            t.connectedPeers().map { peer ->
                PeerDetails(
                    address = peer.address,
                    transportType = peer.transportType,
                    connected = peer.connected
                )
            }
        }
    }

    /**
     * Get peer count
     */
    suspend fun getPeerCount(): Result<ULong> = withContext(Dispatchers.IO) {
        runCatching {
            transport?.peerCount() ?: 0UL
        }
    }

    /**
     * Check if transport is connected
     */
    fun isTransportConnected(): Boolean = transport?.isConnected() ?: false

    /**
     * Get transport bind address
     */
    fun getBindAddress(): String? = transport?.bindAddress()

    /**
     * Send data to a peer
     */
    suspend fun sendToPeer(address: String, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val t = transport ?: throw IllegalStateException("Transport not started")
            t.send(address, data)
        }
    }

    /**
     * Sync with a peer (send our state and merge theirs)
     */
    suspend fun syncWithPeer(peerAddress: String): Result<MergeStats> = withContext(Dispatchers.IO) {
        runCatching {
            val node = meshNode ?: throw IllegalStateException("Mesh node not initialized")
            val t = transport ?: throw IllegalStateException("Transport not started")

            // Get our state and send to peer
            val ourState = node.getState()
            t.send(peerAddress, ourState)

            // In a real implementation, we'd receive the peer's state
            // For now, return empty merge stats
            MergeStats(newEntries = 0UL, totalEntries = node.iouCount())
        }
    }

    fun isTransportStarted(): Boolean = transport != null

    /**
     * Get local mesh state as bytes (for sending to peers)
     */
    suspend fun getLocalState(): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val node = meshNode ?: throw IllegalStateException("Mesh node not initialized")
            node.getState()
        }
    }

    /**
     * Merge remote state from another node
     */
    suspend fun mergeRemoteState(remoteState: ByteArray): Result<MergeStats> = withContext(Dispatchers.IO) {
        runCatching {
            val node = meshNode ?: throw IllegalStateException("Mesh node not initialized")
            val result = node.mergeState(remoteState)
            MergeStats(
                newEntries = result.newEntries,
                totalEntries = result.totalEntries
            )
        }
    }

    /**
     * Get delta (what we have that remote doesn't)
     */
    suspend fun getDelta(remoteState: ByteArray): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val node = meshNode ?: throw IllegalStateException("Mesh node not initialized")
            node.getDelta(remoteState)
        }
    }

    /**
     * Get sync statistics
     */
    suspend fun getSyncStats(): Result<SyncStatistics> = withContext(Dispatchers.IO) {
        runCatching {
            val node = meshNode ?: throw IllegalStateException("Mesh node not initialized")
            val stats = node.stats()
            SyncStatistics(
                totalIous = stats.totalIous,
                totalSyncs = stats.totalSyncs,
                lastSyncTimestamp = stats.lastSyncTimestamp
            )
        }
    }

    /**
     * Get IOU count in mesh
     */
    suspend fun getIouCount(): Result<ULong> = withContext(Dispatchers.IO) {
        runCatching {
            val node = meshNode ?: throw IllegalStateException("Mesh node not initialized")
            node.iouCount()
        }
    }

    fun isMeshInitialized(): Boolean = meshNode != null

    /**
     * Broadcast current mesh state to all connected peers
     * Used after creating a payment to propagate it through the network
     */
    suspend fun broadcastPayment(iou: SignedIou): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val node = meshNode ?: throw IllegalStateException("Mesh node not initialized")
            val t = transport ?: throw IllegalStateException("Transport not started")

            // Get current state (which should include the payment after it's marked as sent)
            val state = node.getState()

            // Send to all connected peers
            val peers = t.connectedPeers()
            var sentCount = 0

            for (peer in peers) {
                try {
                    t.send(peer.address, state)
                    sentCount++
                    Log.d(TAG, "Broadcast state to ${peer.address}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send to ${peer.address}: ${e.message}")
                }
            }

            if (sentCount == 0 && peers.isNotEmpty()) {
                throw Exception("Failed to send to any peers")
            }

            sentCount
        }
    }

    /**
     * Check if we have any connected peers
     */
    fun hasConnectedPeers(): Boolean {
        return try {
            transport?.peerCount()?.let { it > 0UL } ?: false
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "MeshRepository"
    }
}

data class MergeStats(
    val newEntries: ULong,
    val totalEntries: ULong
)

data class SyncStatistics(
    val totalIous: ULong,
    val totalSyncs: ULong,
    val lastSyncTimestamp: ULong
)

data class PeerDetails(
    val address: String,
    val transportType: String,
    val connected: Boolean
)
