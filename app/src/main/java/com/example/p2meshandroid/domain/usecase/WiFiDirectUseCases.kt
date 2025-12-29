package com.example.p2meshandroid.domain.usecase

import android.net.wifi.p2p.WifiP2pInfo
import com.example.p2meshandroid.data.repository.MergeStats
import com.example.p2meshandroid.data.repository.WiFiDirectRepository
import com.example.p2meshandroid.data.wifidirect.WiFiDirectPeer
import com.example.p2meshandroid.data.wifidirect.WiFiDirectState
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for initializing WiFi Direct
 */
class InitializeWiFiDirectUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    suspend operator fun invoke(): Result<Unit> = wifiDirectRepository.initialize()
}

/**
 * Use case for starting WiFi Direct discovery
 */
class StartWiFiDirectDiscoveryUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    suspend operator fun invoke(): Result<Unit> = wifiDirectRepository.startDiscovery()
}

/**
 * Use case for stopping WiFi Direct discovery
 */
class StopWiFiDirectDiscoveryUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    suspend operator fun invoke(): Result<Unit> = wifiDirectRepository.stopDiscovery()
}

/**
 * Use case for connecting to a WiFi Direct peer
 */
class ConnectToWiFiDirectPeerUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    suspend operator fun invoke(deviceAddress: String): Result<Unit> =
        wifiDirectRepository.connectToPeer(deviceAddress)
}

/**
 * Use case for disconnecting from WiFi Direct group
 */
class DisconnectWiFiDirectUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    suspend operator fun invoke(): Result<Unit> = wifiDirectRepository.disconnect()
}

/**
 * Use case for creating a WiFi Direct group
 */
class CreateWiFiDirectGroupUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    suspend operator fun invoke(): Result<Unit> = wifiDirectRepository.createGroup()
}

/**
 * Use case for getting WiFi Direct state
 */
class GetWiFiDirectStateUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    operator fun invoke(): StateFlow<WiFiDirectState> = wifiDirectRepository.state
}

/**
 * Use case for getting discovered WiFi Direct peers
 */
class GetDiscoveredWiFiDirectPeersUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    operator fun invoke(): StateFlow<List<WiFiDirectPeer>> = wifiDirectRepository.discoveredPeers
}

/**
 * Use case for getting WiFi Direct connection info
 */
class GetWiFiDirectConnectionInfoUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    operator fun invoke(): StateFlow<WifiP2pInfo?> = wifiDirectRepository.connectionInfo
}

/**
 * Use case for syncing mesh state via WiFi Direct
 */
class SyncViaWiFiDirectUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    suspend operator fun invoke(): Result<MergeStats> = wifiDirectRepository.syncWithPeers()
}

/**
 * Use case for checking WiFi Direct availability
 */
class CheckWiFiDirectAvailabilityUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    operator fun invoke(): Boolean = wifiDirectRepository.isAvailable()
}

/**
 * Use case for sending data via WiFi Direct
 */
class SendViaWiFiDirectUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    suspend operator fun invoke(address: String, data: ByteArray): Result<Unit> =
        wifiDirectRepository.sendData(address, data)
}

/**
 * Use case for broadcasting data to all WiFi Direct peers
 */
class BroadcastViaWiFiDirectUseCase(
    private val wifiDirectRepository: WiFiDirectRepository
) {
    suspend operator fun invoke(data: ByteArray): Result<Int> =
        wifiDirectRepository.broadcastData(data)
}
