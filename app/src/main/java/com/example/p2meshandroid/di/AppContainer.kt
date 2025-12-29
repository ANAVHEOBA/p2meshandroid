package com.example.p2meshandroid.di

import android.content.Context
import com.example.p2meshandroid.data.repository.BluetoothRepository
import com.example.p2meshandroid.data.repository.MeshRepository
import com.example.p2meshandroid.data.repository.PaymentQueueRepository
import com.example.p2meshandroid.data.repository.SettlementRepository
import com.example.p2meshandroid.data.repository.WalletRepository
import com.example.p2meshandroid.data.repository.WiFiDirectRepository
import com.example.p2meshandroid.data.storage.PaymentQueueStorage
import com.example.p2meshandroid.data.storage.WalletStorage
import com.example.p2meshandroid.domain.usecase.*
import com.example.p2meshandroid.presentation.mesh.MeshViewModel
import com.example.p2meshandroid.presentation.settlement.SettlementViewModel
import com.example.p2meshandroid.presentation.wallet.WalletViewModel

/**
 * Simple dependency injection container.
 * In a production app, consider using Hilt or Koin.
 */
object AppContainer {

    private var walletStorage: WalletStorage? = null
    private var appContext: Context? = null

    /**
     * Initialize the container with Android Context.
     * Call this from Application.onCreate() or MainActivity.onCreate()
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (walletStorage == null) {
            walletStorage = WalletStorage(context.applicationContext)
        }
    }

    // Repositories (singletons)
    val walletRepository: WalletRepository by lazy {
        WalletRepository(walletStorage)
    }

    val meshRepository: MeshRepository by lazy {
        MeshRepository(walletRepository)
    }

    val settlementRepository: SettlementRepository by lazy {
        SettlementRepository(walletRepository)
    }

    val bluetoothRepository: BluetoothRepository by lazy {
        BluetoothRepository(
            context = appContext ?: throw IllegalStateException("AppContainer not initialized"),
            meshRepository = meshRepository
        )
    }

    val wifiDirectRepository: WiFiDirectRepository by lazy {
        WiFiDirectRepository(
            context = appContext ?: throw IllegalStateException("AppContainer not initialized"),
            meshRepository = meshRepository
        )
    }

    private val paymentQueueStorage: PaymentQueueStorage by lazy {
        PaymentQueueStorage(appContext ?: throw IllegalStateException("AppContainer not initialized"))
    }

    val paymentQueueRepository: PaymentQueueRepository by lazy {
        PaymentQueueRepository(
            storage = paymentQueueStorage,
            walletRepository = walletRepository,
            meshRepository = meshRepository
        )
    }

    // Use Cases
    val loadWalletUseCase: LoadWalletUseCase by lazy {
        LoadWalletUseCase(walletRepository)
    }

    val createWalletUseCase: CreateWalletUseCase by lazy {
        CreateWalletUseCase(walletRepository)
    }

    val restoreWalletUseCase: RestoreWalletUseCase by lazy {
        RestoreWalletUseCase(walletRepository)
    }

    val getWalletInfoUseCase: GetWalletInfoUseCase by lazy {
        GetWalletInfoUseCase(walletRepository)
    }

    val sendPaymentUseCase: SendPaymentUseCase by lazy {
        SendPaymentUseCase(walletRepository)
    }

    val receivePaymentUseCase: ReceivePaymentUseCase by lazy {
        ReceivePaymentUseCase(walletRepository)
    }

    val getPendingPaymentsUseCase: GetPendingPaymentsUseCase by lazy {
        GetPendingPaymentsUseCase(walletRepository)
    }

    val backupWalletUseCase: BackupWalletUseCase by lazy {
        BackupWalletUseCase(walletRepository)
    }

    val fundFromFaucetUseCase: FundFromFaucetUseCase by lazy {
        FundFromFaucetUseCase(walletRepository)
    }

    // Mesh Use Cases
    val startMeshTransportUseCase: StartMeshTransportUseCase by lazy {
        StartMeshTransportUseCase(meshRepository)
    }

    val stopMeshTransportUseCase: StopMeshTransportUseCase by lazy {
        StopMeshTransportUseCase(meshRepository)
    }

    val connectToPeerUseCase: ConnectToPeerUseCase by lazy {
        ConnectToPeerUseCase(meshRepository)
    }

    val disconnectFromPeerUseCase: DisconnectFromPeerUseCase by lazy {
        DisconnectFromPeerUseCase(meshRepository)
    }

    val getConnectedPeersUseCase: GetConnectedPeersUseCase by lazy {
        GetConnectedPeersUseCase(meshRepository)
    }

    val getMeshStatsUseCase: GetMeshStatsUseCase by lazy {
        GetMeshStatsUseCase(meshRepository)
    }

    val syncWithPeerUseCase: SyncWithPeerUseCase by lazy {
        SyncWithPeerUseCase(meshRepository)
    }

    // Bluetooth Use Cases
    val startBluetoothMeshUseCase: StartBluetoothMeshUseCase by lazy {
        StartBluetoothMeshUseCase(bluetoothRepository)
    }

    val stopBluetoothMeshUseCase: StopBluetoothMeshUseCase by lazy {
        StopBluetoothMeshUseCase(bluetoothRepository)
    }

    val connectToBluetoothPeerUseCase: ConnectToBluetoothPeerUseCase by lazy {
        ConnectToBluetoothPeerUseCase(bluetoothRepository)
    }

    val disconnectFromBluetoothPeerUseCase: DisconnectFromBluetoothPeerUseCase by lazy {
        DisconnectFromBluetoothPeerUseCase(bluetoothRepository)
    }

    val syncWithBluetoothPeerUseCase: SyncWithBluetoothPeerUseCase by lazy {
        SyncWithBluetoothPeerUseCase(bluetoothRepository)
    }

    val getBluetoothStateUseCase: GetBluetoothStateUseCase by lazy {
        GetBluetoothStateUseCase(bluetoothRepository)
    }

    val getDiscoveredBluetoothPeersUseCase: GetDiscoveredBluetoothPeersUseCase by lazy {
        GetDiscoveredBluetoothPeersUseCase(bluetoothRepository)
    }

    val getConnectedBluetoothPeersUseCase: GetConnectedBluetoothPeersUseCase by lazy {
        GetConnectedBluetoothPeersUseCase(bluetoothRepository)
    }

    val checkBluetoothAvailabilityUseCase: CheckBluetoothAvailabilityUseCase by lazy {
        CheckBluetoothAvailabilityUseCase(bluetoothRepository)
    }

    // WiFi Direct Use Cases
    val initializeWiFiDirectUseCase: InitializeWiFiDirectUseCase by lazy {
        InitializeWiFiDirectUseCase(wifiDirectRepository)
    }

    val startWiFiDirectDiscoveryUseCase: StartWiFiDirectDiscoveryUseCase by lazy {
        StartWiFiDirectDiscoveryUseCase(wifiDirectRepository)
    }

    val stopWiFiDirectDiscoveryUseCase: StopWiFiDirectDiscoveryUseCase by lazy {
        StopWiFiDirectDiscoveryUseCase(wifiDirectRepository)
    }

    val connectToWiFiDirectPeerUseCase: ConnectToWiFiDirectPeerUseCase by lazy {
        ConnectToWiFiDirectPeerUseCase(wifiDirectRepository)
    }

    val disconnectWiFiDirectUseCase: DisconnectWiFiDirectUseCase by lazy {
        DisconnectWiFiDirectUseCase(wifiDirectRepository)
    }

    val createWiFiDirectGroupUseCase: CreateWiFiDirectGroupUseCase by lazy {
        CreateWiFiDirectGroupUseCase(wifiDirectRepository)
    }

    val getWiFiDirectStateUseCase: GetWiFiDirectStateUseCase by lazy {
        GetWiFiDirectStateUseCase(wifiDirectRepository)
    }

    val getDiscoveredWiFiDirectPeersUseCase: GetDiscoveredWiFiDirectPeersUseCase by lazy {
        GetDiscoveredWiFiDirectPeersUseCase(wifiDirectRepository)
    }

    val syncViaWiFiDirectUseCase: SyncViaWiFiDirectUseCase by lazy {
        SyncViaWiFiDirectUseCase(wifiDirectRepository)
    }

    val checkWiFiDirectAvailabilityUseCase: CheckWiFiDirectAvailabilityUseCase by lazy {
        CheckWiFiDirectAvailabilityUseCase(wifiDirectRepository)
    }

    // Settlement Use Cases
    val initializeSettlementUseCase: InitializeSettlementUseCase by lazy {
        InitializeSettlementUseCase(settlementRepository)
    }

    val collectIousUseCase: CollectIousUseCase by lazy {
        CollectIousUseCase(settlementRepository)
    }

    val createSettlementBatchUseCase: CreateSettlementBatchUseCase by lazy {
        CreateSettlementBatchUseCase(settlementRepository)
    }

    val submitSettlementBatchUseCase: SubmitSettlementBatchUseCase by lazy {
        SubmitSettlementBatchUseCase(settlementRepository)
    }

    val getSettlementStatsUseCase: GetSettlementStatsUseCase by lazy {
        GetSettlementStatsUseCase(settlementRepository)
    }

    val simulateSettlementUseCase: SimulateSettlementUseCase by lazy {
        SimulateSettlementUseCase(settlementRepository)
    }

    val clearSettlementDataUseCase: ClearSettlementDataUseCase by lazy {
        ClearSettlementDataUseCase(settlementRepository)
    }

    // Payment Queue Use Cases
    val queuePaymentUseCase: QueuePaymentUseCase by lazy {
        QueuePaymentUseCase(paymentQueueRepository)
    }

    val processPaymentQueueUseCase: ProcessPaymentQueueUseCase by lazy {
        ProcessPaymentQueueUseCase(paymentQueueRepository)
    }

    val getQueuedPaymentsUseCase: GetQueuedPaymentsUseCase by lazy {
        GetQueuedPaymentsUseCase(paymentQueueRepository)
    }

    val getQueueStatsUseCase: GetQueueStatsUseCase by lazy {
        GetQueueStatsUseCase(paymentQueueRepository)
    }

    val retryQueuedPaymentUseCase: RetryQueuedPaymentUseCase by lazy {
        RetryQueuedPaymentUseCase(paymentQueueRepository)
    }

    val cancelQueuedPaymentUseCase: CancelQueuedPaymentUseCase by lazy {
        CancelQueuedPaymentUseCase(paymentQueueRepository)
    }

    val clearPaymentQueueUseCase: ClearPaymentQueueUseCase by lazy {
        ClearPaymentQueueUseCase(paymentQueueRepository)
    }

    val startQueueAutoProcessUseCase: StartQueueAutoProcessUseCase by lazy {
        StartQueueAutoProcessUseCase(paymentQueueRepository)
    }

    val stopQueueAutoProcessUseCase: StopQueueAutoProcessUseCase by lazy {
        StopQueueAutoProcessUseCase(paymentQueueRepository)
    }

    val hasPendingQueuedPaymentsUseCase: HasPendingQueuedPaymentsUseCase by lazy {
        HasPendingQueuedPaymentsUseCase(paymentQueueRepository)
    }

    val notifyConnectivityRestoredUseCase: NotifyConnectivityRestoredUseCase by lazy {
        NotifyConnectivityRestoredUseCase(paymentQueueRepository)
    }

    val getQueueProcessingStateUseCase: GetQueueProcessingStateUseCase by lazy {
        GetQueueProcessingStateUseCase(paymentQueueRepository)
    }

    // ViewModel Factories
    fun provideWalletViewModel(): WalletViewModel {
        return WalletViewModel(
            loadWalletUseCase = loadWalletUseCase,
            createWalletUseCase = createWalletUseCase,
            getWalletInfoUseCase = getWalletInfoUseCase,
            sendPaymentUseCase = sendPaymentUseCase,
            receivePaymentUseCase = receivePaymentUseCase,
            getPendingPaymentsUseCase = getPendingPaymentsUseCase,
            backupWalletUseCase = backupWalletUseCase,
            fundFromFaucetUseCase = fundFromFaucetUseCase,
            restoreWalletUseCase = restoreWalletUseCase,
            // Payment Queue use cases
            queuePaymentUseCase = queuePaymentUseCase,
            processPaymentQueueUseCase = processPaymentQueueUseCase,
            getQueuedPaymentsUseCase = getQueuedPaymentsUseCase,
            getQueueStatsUseCase = getQueueStatsUseCase,
            retryQueuedPaymentUseCase = retryQueuedPaymentUseCase,
            cancelQueuedPaymentUseCase = cancelQueuedPaymentUseCase,
            getQueueProcessingStateUseCase = getQueueProcessingStateUseCase,
            meshRepository = meshRepository
        )
    }

    fun provideMeshViewModel(): MeshViewModel {
        return MeshViewModel(
            startMeshTransportUseCase = startMeshTransportUseCase,
            stopMeshTransportUseCase = stopMeshTransportUseCase,
            connectToPeerUseCase = connectToPeerUseCase,
            disconnectFromPeerUseCase = disconnectFromPeerUseCase,
            getConnectedPeersUseCase = getConnectedPeersUseCase,
            getMeshStatsUseCase = getMeshStatsUseCase,
            syncWithPeerUseCase = syncWithPeerUseCase,
            // Bluetooth use cases
            startBluetoothMeshUseCase = startBluetoothMeshUseCase,
            stopBluetoothMeshUseCase = stopBluetoothMeshUseCase,
            connectToBluetoothPeerUseCase = connectToBluetoothPeerUseCase,
            disconnectFromBluetoothPeerUseCase = disconnectFromBluetoothPeerUseCase,
            syncWithBluetoothPeerUseCase = syncWithBluetoothPeerUseCase,
            getBluetoothStateUseCase = getBluetoothStateUseCase,
            getDiscoveredBluetoothPeersUseCase = getDiscoveredBluetoothPeersUseCase,
            getConnectedBluetoothPeersUseCase = getConnectedBluetoothPeersUseCase,
            checkBluetoothAvailabilityUseCase = checkBluetoothAvailabilityUseCase,
            // WiFi Direct use cases
            initializeWiFiDirectUseCase = initializeWiFiDirectUseCase,
            startWiFiDirectDiscoveryUseCase = startWiFiDirectDiscoveryUseCase,
            stopWiFiDirectDiscoveryUseCase = stopWiFiDirectDiscoveryUseCase,
            connectToWiFiDirectPeerUseCase = connectToWiFiDirectPeerUseCase,
            disconnectWiFiDirectUseCase = disconnectWiFiDirectUseCase,
            createWiFiDirectGroupUseCase = createWiFiDirectGroupUseCase,
            getWiFiDirectStateUseCase = getWiFiDirectStateUseCase,
            getDiscoveredWiFiDirectPeersUseCase = getDiscoveredWiFiDirectPeersUseCase,
            syncViaWiFiDirectUseCase = syncViaWiFiDirectUseCase,
            checkWiFiDirectAvailabilityUseCase = checkWiFiDirectAvailabilityUseCase
        )
    }

    fun provideSettlementViewModel(): SettlementViewModel {
        return SettlementViewModel(
            walletRepository = walletRepository,
            initializeSettlementUseCase = initializeSettlementUseCase,
            collectIousUseCase = collectIousUseCase,
            createSettlementBatchUseCase = createSettlementBatchUseCase,
            submitSettlementBatchUseCase = submitSettlementBatchUseCase,
            getSettlementStatsUseCase = getSettlementStatsUseCase,
            simulateSettlementUseCase = simulateSettlementUseCase,
            clearSettlementDataUseCase = clearSettlementDataUseCase
        )
    }
}

/**
 * ViewModel factory for Compose
 */
@Suppress("UNCHECKED_CAST")
class WalletViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
            return AppContainer.provideWalletViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Suppress("UNCHECKED_CAST")
class MeshViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeshViewModel::class.java)) {
            return AppContainer.provideMeshViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Suppress("UNCHECKED_CAST")
class SettlementViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettlementViewModel::class.java)) {
            return AppContainer.provideSettlementViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
