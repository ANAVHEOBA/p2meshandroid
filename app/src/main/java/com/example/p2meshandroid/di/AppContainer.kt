package com.example.p2meshandroid.di

import android.content.Context
import com.example.p2meshandroid.data.repository.MeshRepository
import com.example.p2meshandroid.data.repository.WalletRepository
import com.example.p2meshandroid.data.storage.WalletStorage
import com.example.p2meshandroid.domain.usecase.*
import com.example.p2meshandroid.presentation.mesh.MeshViewModel
import com.example.p2meshandroid.presentation.wallet.WalletViewModel

/**
 * Simple dependency injection container.
 * In a production app, consider using Hilt or Koin.
 */
object AppContainer {

    private var walletStorage: WalletStorage? = null

    /**
     * Initialize the container with Android Context.
     * Call this from Application.onCreate() or MainActivity.onCreate()
     */
    fun initialize(context: Context) {
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
            fundFromFaucetUseCase = fundFromFaucetUseCase
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
            syncWithPeerUseCase = syncWithPeerUseCase
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
