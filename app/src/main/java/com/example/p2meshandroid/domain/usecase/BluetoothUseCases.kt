package com.example.p2meshandroid.domain.usecase

import com.example.p2meshandroid.data.bluetooth.BluetoothPeer
import com.example.p2meshandroid.data.bluetooth.BluetoothState
import com.example.p2meshandroid.data.repository.BluetoothRepository
import com.example.p2meshandroid.data.repository.MergeStats
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for starting Bluetooth mesh mode
 */
class StartBluetoothMeshUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(): Result<Unit> = bluetoothRepository.startMeshMode()
}

/**
 * Use case for stopping Bluetooth mesh mode
 */
class StopBluetoothMeshUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(): Result<Unit> = bluetoothRepository.stopMeshMode()
}

/**
 * Use case for starting Bluetooth scanning
 */
class StartBluetoothScanUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(): Result<Unit> = bluetoothRepository.startScanning()
}

/**
 * Use case for stopping Bluetooth scanning
 */
class StopBluetoothScanUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(): Result<Unit> = bluetoothRepository.stopScanning()
}

/**
 * Use case for starting Bluetooth advertising
 */
class StartBluetoothAdvertiseUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(): Result<Unit> = bluetoothRepository.startAdvertising()
}

/**
 * Use case for stopping Bluetooth advertising
 */
class StopBluetoothAdvertiseUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(): Result<Unit> = bluetoothRepository.stopAdvertising()
}

/**
 * Use case for connecting to a Bluetooth peer
 */
class ConnectToBluetoothPeerUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(address: String): Result<Unit> =
        bluetoothRepository.connectToPeer(address)
}

/**
 * Use case for disconnecting from a Bluetooth peer
 */
class DisconnectFromBluetoothPeerUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(address: String): Result<Unit> =
        bluetoothRepository.disconnectFromPeer(address)
}

/**
 * Use case for syncing with a Bluetooth peer
 */
class SyncWithBluetoothPeerUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(address: String): Result<MergeStats> =
        bluetoothRepository.syncWithPeer(address)
}

/**
 * Use case for getting Bluetooth state
 */
class GetBluetoothStateUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    operator fun invoke(): StateFlow<BluetoothState> = bluetoothRepository.state
}

/**
 * Use case for getting discovered Bluetooth peers
 */
class GetDiscoveredBluetoothPeersUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    operator fun invoke(): StateFlow<List<BluetoothPeer>> = bluetoothRepository.discoveredPeers
}

/**
 * Use case for getting connected Bluetooth peers
 */
class GetConnectedBluetoothPeersUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    operator fun invoke(): StateFlow<List<BluetoothPeer>> = bluetoothRepository.connectedPeers
}

/**
 * Use case for checking Bluetooth availability
 */
class CheckBluetoothAvailabilityUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    data class BluetoothAvailability(
        val isAvailable: Boolean,
        val isEnabled: Boolean
    )

    operator fun invoke(): BluetoothAvailability = BluetoothAvailability(
        isAvailable = bluetoothRepository.isBluetoothAvailable(),
        isEnabled = bluetoothRepository.isBluetoothEnabled()
    )
}

/**
 * Use case for broadcasting data to all Bluetooth peers
 */
class BroadcastToBluetoothPeersUseCase(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(data: ByteArray): Result<Int> =
        bluetoothRepository.broadcastData(data)
}
