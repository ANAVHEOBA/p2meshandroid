package com.example.p2meshandroid.domain.usecase

import com.example.p2meshandroid.data.repository.MeshRepository
import com.example.p2meshandroid.data.repository.MergeStats
import com.example.p2meshandroid.data.repository.PeerDetails
import com.example.p2meshandroid.data.repository.SyncStatistics

/**
 * Use case for starting the mesh transport
 */
class StartMeshTransportUseCase(
    private val meshRepository: MeshRepository
) {
    suspend operator fun invoke(bindAddress: String = "0.0.0.0:0"): Result<String> {
        return meshRepository.startTransport(bindAddress)
    }
}

/**
 * Use case for stopping the mesh transport
 */
class StopMeshTransportUseCase(
    private val meshRepository: MeshRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return meshRepository.stopTransport()
    }
}

/**
 * Use case for connecting to a peer
 */
class ConnectToPeerUseCase(
    private val meshRepository: MeshRepository
) {
    suspend operator fun invoke(address: String): Result<Unit> {
        return meshRepository.connectToPeer(address)
    }
}

/**
 * Use case for disconnecting from a peer
 */
class DisconnectFromPeerUseCase(
    private val meshRepository: MeshRepository
) {
    suspend operator fun invoke(address: String): Result<Unit> {
        return meshRepository.disconnectFromPeer(address)
    }
}

/**
 * Use case for getting connected peers
 */
class GetConnectedPeersUseCase(
    private val meshRepository: MeshRepository
) {
    suspend operator fun invoke(): Result<List<PeerDetails>> {
        return meshRepository.getConnectedPeers()
    }
}

/**
 * Use case for getting mesh statistics
 */
class GetMeshStatsUseCase(
    private val meshRepository: MeshRepository
) {
    suspend operator fun invoke(): Result<MeshStats> {
        val syncStats = meshRepository.getSyncStats().getOrNull()
        val iouCount = meshRepository.getIouCount().getOrNull() ?: 0UL
        val peerCount = meshRepository.getPeerCount().getOrNull() ?: 0UL

        return Result.success(
            MeshStats(
                peerCount = peerCount,
                iouCount = iouCount,
                totalSyncs = syncStats?.totalSyncs ?: 0UL,
                lastSyncTimestamp = syncStats?.lastSyncTimestamp ?: 0UL,
                isTransportRunning = meshRepository.isTransportStarted(),
                bindAddress = meshRepository.getBindAddress()
            )
        )
    }
}

/**
 * Use case for syncing with a peer
 */
class SyncWithPeerUseCase(
    private val meshRepository: MeshRepository
) {
    suspend operator fun invoke(peerAddress: String): Result<MergeStats> {
        return meshRepository.syncWithPeer(peerAddress)
    }
}

/**
 * Combined mesh stats
 */
data class MeshStats(
    val peerCount: ULong,
    val iouCount: ULong,
    val totalSyncs: ULong,
    val lastSyncTimestamp: ULong,
    val isTransportRunning: Boolean,
    val bindAddress: String?
)
