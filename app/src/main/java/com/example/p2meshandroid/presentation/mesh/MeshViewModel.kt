package com.example.p2meshandroid.presentation.mesh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2meshandroid.data.bluetooth.BluetoothPeer
import com.example.p2meshandroid.data.bluetooth.BluetoothState
import com.example.p2meshandroid.data.repository.PeerDetails
import com.example.p2meshandroid.data.wifidirect.WiFiDirectPeer
import com.example.p2meshandroid.data.wifidirect.WiFiDirectState
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
    private val syncWithPeerUseCase: SyncWithPeerUseCase,
    // Bluetooth use cases
    private val startBluetoothMeshUseCase: StartBluetoothMeshUseCase? = null,
    private val stopBluetoothMeshUseCase: StopBluetoothMeshUseCase? = null,
    private val connectToBluetoothPeerUseCase: ConnectToBluetoothPeerUseCase? = null,
    private val disconnectFromBluetoothPeerUseCase: DisconnectFromBluetoothPeerUseCase? = null,
    private val syncWithBluetoothPeerUseCase: SyncWithBluetoothPeerUseCase? = null,
    private val getBluetoothStateUseCase: GetBluetoothStateUseCase? = null,
    private val getDiscoveredBluetoothPeersUseCase: GetDiscoveredBluetoothPeersUseCase? = null,
    private val getConnectedBluetoothPeersUseCase: GetConnectedBluetoothPeersUseCase? = null,
    private val checkBluetoothAvailabilityUseCase: CheckBluetoothAvailabilityUseCase? = null,
    // WiFi Direct use cases
    private val initializeWiFiDirectUseCase: InitializeWiFiDirectUseCase? = null,
    private val startWiFiDirectDiscoveryUseCase: StartWiFiDirectDiscoveryUseCase? = null,
    private val stopWiFiDirectDiscoveryUseCase: StopWiFiDirectDiscoveryUseCase? = null,
    private val connectToWiFiDirectPeerUseCase: ConnectToWiFiDirectPeerUseCase? = null,
    private val disconnectWiFiDirectUseCase: DisconnectWiFiDirectUseCase? = null,
    private val createWiFiDirectGroupUseCase: CreateWiFiDirectGroupUseCase? = null,
    private val getWiFiDirectStateUseCase: GetWiFiDirectStateUseCase? = null,
    private val getDiscoveredWiFiDirectPeersUseCase: GetDiscoveredWiFiDirectPeersUseCase? = null,
    private val syncViaWiFiDirectUseCase: SyncViaWiFiDirectUseCase? = null,
    private val checkWiFiDirectAvailabilityUseCase: CheckWiFiDirectAvailabilityUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<MeshUiState>(MeshUiState.Disconnected())
    val uiState: StateFlow<MeshUiState> = _uiState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _bluetoothState = MutableStateFlow<BluetoothUiState>(BluetoothUiState.Unavailable)
    val bluetoothState: StateFlow<BluetoothUiState> = _bluetoothState.asStateFlow()

    private val _discoveredBluetoothPeers = MutableStateFlow<List<BluetoothPeer>>(emptyList())
    val discoveredBluetoothPeers: StateFlow<List<BluetoothPeer>> = _discoveredBluetoothPeers.asStateFlow()

    private val _connectedBluetoothPeers = MutableStateFlow<List<BluetoothPeer>>(emptyList())
    val connectedBluetoothPeers: StateFlow<List<BluetoothPeer>> = _connectedBluetoothPeers.asStateFlow()

    // WiFi Direct state
    private val _wifiDirectState = MutableStateFlow<WiFiDirectUiState>(WiFiDirectUiState.Unavailable)
    val wifiDirectState: StateFlow<WiFiDirectUiState> = _wifiDirectState.asStateFlow()

    private val _discoveredWiFiDirectPeers = MutableStateFlow<List<WiFiDirectPeer>>(emptyList())
    val discoveredWiFiDirectPeers: StateFlow<List<WiFiDirectPeer>> = _discoveredWiFiDirectPeers.asStateFlow()

    init {
        refreshStats()
        checkBluetoothAvailability()
        observeBluetoothState()
        initializeWiFiDirect()
        observeWiFiDirectState()
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

    // ========== Bluetooth Functions ==========

    private fun checkBluetoothAvailability() {
        val availability = checkBluetoothAvailabilityUseCase?.invoke()
        _bluetoothState.value = when {
            availability == null -> BluetoothUiState.Unavailable
            !availability.isAvailable -> BluetoothUiState.Unavailable
            !availability.isEnabled -> BluetoothUiState.Disabled
            else -> BluetoothUiState.Ready
        }
    }

    private fun observeBluetoothState() {
        viewModelScope.launch {
            getBluetoothStateUseCase?.invoke()?.collect { state ->
                _bluetoothState.value = when (state) {
                    is BluetoothState.Disabled -> BluetoothUiState.Disabled
                    is BluetoothState.Idle -> BluetoothUiState.Ready
                    is BluetoothState.Scanning -> BluetoothUiState.Scanning
                    is BluetoothState.Advertising -> BluetoothUiState.Advertising
                    is BluetoothState.ScanningAndAdvertising -> BluetoothUiState.Active
                    is BluetoothState.Error -> BluetoothUiState.Error(state.message)
                    is BluetoothState.Enabling -> BluetoothUiState.Enabling
                }
            }
        }

        viewModelScope.launch {
            getDiscoveredBluetoothPeersUseCase?.invoke()?.collect { peers ->
                _discoveredBluetoothPeers.value = peers
            }
        }

        viewModelScope.launch {
            getConnectedBluetoothPeersUseCase?.invoke()?.collect { peers ->
                _connectedBluetoothPeers.value = peers
            }
        }
    }

    /**
     * Start Bluetooth mesh mode (scan + advertise)
     */
    fun startBluetoothMesh() {
        viewModelScope.launch {
            _bluetoothState.value = BluetoothUiState.Starting
            val result = startBluetoothMeshUseCase?.invoke()
            result?.fold(
                onSuccess = {
                    _bluetoothState.value = BluetoothUiState.Active
                },
                onFailure = { error ->
                    _bluetoothState.value = BluetoothUiState.Error(error.message ?: "Failed to start Bluetooth")
                }
            )
        }
    }

    /**
     * Stop Bluetooth mesh mode
     */
    fun stopBluetoothMesh() {
        viewModelScope.launch {
            stopBluetoothMeshUseCase?.invoke()
            _bluetoothState.value = BluetoothUiState.Ready
        }
    }

    /**
     * Connect to a discovered Bluetooth peer
     */
    fun connectToBluetoothPeer(address: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting(address)
            val result = connectToBluetoothPeerUseCase?.invoke(address)
            result?.fold(
                onSuccess = {
                    _connectionState.value = ConnectionState.Idle
                },
                onFailure = { error ->
                    _connectionState.value = ConnectionState.Error(error.message ?: "Failed to connect")
                }
            )
        }
    }

    /**
     * Disconnect from a Bluetooth peer
     */
    fun disconnectFromBluetoothPeer(address: String) {
        viewModelScope.launch {
            disconnectFromBluetoothPeerUseCase?.invoke(address)
        }
    }

    /**
     * Sync mesh state with a Bluetooth peer
     */
    fun syncWithBluetoothPeer(address: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Syncing(address)
            val result = syncWithBluetoothPeerUseCase?.invoke(address)
            result?.fold(
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
     * Check if Bluetooth is available
     */
    fun isBluetoothAvailable(): Boolean {
        val availability = checkBluetoothAvailabilityUseCase?.invoke()
        return availability?.isAvailable == true
    }

    // ========== WiFi Direct Functions ==========

    private fun initializeWiFiDirect() {
        viewModelScope.launch {
            val result = initializeWiFiDirectUseCase?.invoke()
            result?.fold(
                onSuccess = {
                    _wifiDirectState.value = WiFiDirectUiState.Ready
                },
                onFailure = {
                    _wifiDirectState.value = WiFiDirectUiState.Unavailable
                }
            )
        }
    }

    private fun observeWiFiDirectState() {
        viewModelScope.launch {
            getWiFiDirectStateUseCase?.invoke()?.collect { state ->
                _wifiDirectState.value = when (state) {
                    is WiFiDirectState.Disabled -> WiFiDirectUiState.Disabled
                    is WiFiDirectState.Idle -> WiFiDirectUiState.Ready
                    is WiFiDirectState.Discovering -> WiFiDirectUiState.Discovering
                    is WiFiDirectState.Connecting -> WiFiDirectUiState.Connecting
                    is WiFiDirectState.Connected -> WiFiDirectUiState.Connected
                    is WiFiDirectState.GroupFormed -> WiFiDirectUiState.GroupFormed(
                        isOwner = state.isOwner,
                        ownerAddress = state.ownerAddress
                    )
                    is WiFiDirectState.Error -> WiFiDirectUiState.Error(state.message)
                }
            }
        }

        viewModelScope.launch {
            getDiscoveredWiFiDirectPeersUseCase?.invoke()?.collect { peers ->
                _discoveredWiFiDirectPeers.value = peers
            }
        }
    }

    /**
     * Start WiFi Direct discovery
     */
    fun startWiFiDirectDiscovery() {
        viewModelScope.launch {
            _wifiDirectState.value = WiFiDirectUiState.Starting
            val result = startWiFiDirectDiscoveryUseCase?.invoke()
            result?.fold(
                onSuccess = {
                    _wifiDirectState.value = WiFiDirectUiState.Discovering
                },
                onFailure = { error ->
                    _wifiDirectState.value = WiFiDirectUiState.Error(error.message ?: "Failed to start discovery")
                }
            )
        }
    }

    /**
     * Stop WiFi Direct discovery
     */
    fun stopWiFiDirectDiscovery() {
        viewModelScope.launch {
            stopWiFiDirectDiscoveryUseCase?.invoke()
            _wifiDirectState.value = WiFiDirectUiState.Ready
        }
    }

    /**
     * Create a WiFi Direct group (become group owner)
     */
    fun createWiFiDirectGroup() {
        viewModelScope.launch {
            _wifiDirectState.value = WiFiDirectUiState.Connecting
            val result = createWiFiDirectGroupUseCase?.invoke()
            result?.onFailure { error ->
                _wifiDirectState.value = WiFiDirectUiState.Error(error.message ?: "Failed to create group")
            }
        }
    }

    /**
     * Connect to a WiFi Direct peer
     */
    fun connectToWiFiDirectPeer(deviceAddress: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting(deviceAddress)
            val result = connectToWiFiDirectPeerUseCase?.invoke(deviceAddress)
            result?.fold(
                onSuccess = {
                    _connectionState.value = ConnectionState.Idle
                },
                onFailure = { error ->
                    _connectionState.value = ConnectionState.Error(error.message ?: "Failed to connect")
                }
            )
        }
    }

    /**
     * Disconnect from WiFi Direct group
     */
    fun disconnectWiFiDirect() {
        viewModelScope.launch {
            disconnectWiFiDirectUseCase?.invoke()
            _wifiDirectState.value = WiFiDirectUiState.Ready
        }
    }

    /**
     * Sync mesh state via WiFi Direct
     */
    fun syncViaWiFiDirect() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Syncing("WiFi Direct")
            val result = syncViaWiFiDirectUseCase?.invoke()
            result?.fold(
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
     * Check if WiFi Direct is available
     */
    fun isWiFiDirectAvailable(): Boolean {
        return checkWiFiDirectAvailabilityUseCase?.invoke() == true
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

/**
 * UI state for Bluetooth operations
 */
sealed class BluetoothUiState {
    object Unavailable : BluetoothUiState()
    object Disabled : BluetoothUiState()
    object Enabling : BluetoothUiState()
    object Ready : BluetoothUiState()
    object Starting : BluetoothUiState()
    object Scanning : BluetoothUiState()
    object Advertising : BluetoothUiState()
    object Active : BluetoothUiState()
    data class Error(val message: String) : BluetoothUiState()
}

/**
 * UI state for WiFi Direct operations
 */
sealed class WiFiDirectUiState {
    object Unavailable : WiFiDirectUiState()
    object Disabled : WiFiDirectUiState()
    object Ready : WiFiDirectUiState()
    object Starting : WiFiDirectUiState()
    object Discovering : WiFiDirectUiState()
    object Connecting : WiFiDirectUiState()
    object Connected : WiFiDirectUiState()
    data class GroupFormed(val isOwner: Boolean, val ownerAddress: String?) : WiFiDirectUiState()
    data class Error(val message: String) : WiFiDirectUiState()
}
