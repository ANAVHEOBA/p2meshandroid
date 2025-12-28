package com.example.p2meshandroid.presentation.mesh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2meshandroid.data.repository.PeerDetails
import com.example.p2meshandroid.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for mesh network screen
 */
class MeshViewModel(
    private val startMeshTransportUseCase: StartMeshTransportUseCase,
    private val stopMeshTransportUseCase: StopMeshTransportUseCase,
    private val connectToPeerUseCase: ConnectToPeerUseCase,
    private val disconnectFromPeerUseCase: DisconnectFromPeerUseCase,
    private val getConnectedPeersUseCase: GetConnectedPeersUseCase,
    private val getMeshStatsUseCase: GetMeshStatsUseCase,
    private val syncWithPeerUseCase: SyncWithPeerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MeshUiState>(MeshUiState.Disconnected())
    val uiState: StateFlow<MeshUiState> = _uiState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    init {
        refreshStats()
    }

    /**
     * Start the mesh transport
     */
    fun startTransport(bindAddress: String = "0.0.0.0:0") {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Starting

            val result = startMeshTransportUseCase(bindAddress)
            result.fold(
                onSuccess = { address ->
                    _connectionState.value = ConnectionState.Idle
                    refreshStats()
                },
                onFailure = { error ->
                    _connectionState.value = ConnectionState.Error(error.message ?: "Failed to start transport")
                }
            )
        }
    }

    /**
     * Stop the mesh transport
     */
    fun stopTransport() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Stopping

            val result = stopMeshTransportUseCase()
            result.fold(
                onSuccess = {
                    _connectionState.value = ConnectionState.Idle
                    _uiState.value = MeshUiState.Disconnected()
                },
                onFailure = { error ->
                    _connectionState.value = ConnectionState.Error(error.message ?: "Failed to stop transport")
                }
            )
        }
    }

    /**
     * Connect to a peer
     */
    fun connectToPeer(address: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting(address)

            val result = connectToPeerUseCase(address)
            result.fold(
                onSuccess = {
                    _connectionState.value = ConnectionState.Idle
                    refreshStats()
                },
                onFailure = { error ->
                    _connectionState.value = ConnectionState.Error(error.message ?: "Failed to connect")
                }
            )
        }
    }

    /**
     * Disconnect from a peer
     */
    fun disconnectFromPeer(address: String) {
        viewModelScope.launch {
            val result = disconnectFromPeerUseCase(address)
            result.onSuccess {
                refreshStats()
            }
        }
    }

    /**
     * Sync with a peer
     */
    fun syncWithPeer(address: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Syncing(address)

            val result = syncWithPeerUseCase(address)
            result.fold(
                onSuccess = { stats ->
                    _connectionState.value = ConnectionState.SyncComplete(stats.newEntries)
                    refreshStats()
                },
                onFailure = { error ->
                    _connectionState.value = ConnectionState.Error(error.message ?: "Sync failed")
                }
            )
        }
    }

    /**
     * Refresh mesh stats and connected peers
     */
    fun refreshStats() {
        viewModelScope.launch {
            val statsResult = getMeshStatsUseCase()
            val peersResult = getConnectedPeersUseCase()

            val stats = statsResult.getOrNull()
            val peers = peersResult.getOrNull() ?: emptyList()

            if (stats != null) {
                if (stats.isTransportRunning) {
                    _uiState.value = MeshUiState.Connected(
                        bindAddress = stats.bindAddress ?: "",
                        peerCount = stats.peerCount,
                        iouCount = stats.iouCount,
                        totalSyncs = stats.totalSyncs,
                        lastSyncTimestamp = stats.lastSyncTimestamp,
                        connectedPeers = peers
                    )
                } else {
                    _uiState.value = MeshUiState.Disconnected(
                        iouCount = stats.iouCount,
                        totalSyncs = stats.totalSyncs
                    )
                }
            }
        }
    }

    fun resetConnectionState() {
        _connectionState.value = ConnectionState.Idle
    }
}

/**
 * UI state for mesh screen
 */
sealed class MeshUiState {
    data class Disconnected(
        val iouCount: ULong = 0UL,
        val totalSyncs: ULong = 0UL
    ) : MeshUiState()

    data class Connected(
        val bindAddress: String,
        val peerCount: ULong,
        val iouCount: ULong,
        val totalSyncs: ULong,
        val lastSyncTimestamp: ULong,
        val connectedPeers: List<PeerDetails>
    ) : MeshUiState()
}

/**
 * State for connection operations
 */
sealed class ConnectionState {
    object Idle : ConnectionState()
    object Starting : ConnectionState()
    object Stopping : ConnectionState()
    data class Connecting(val address: String) : ConnectionState()
    data class Syncing(val address: String) : ConnectionState()
    data class SyncComplete(val newEntries: ULong) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
