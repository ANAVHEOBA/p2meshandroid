package com.example.p2meshandroid.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2meshandroid.data.repository.MeshRepository
import com.example.p2meshandroid.data.repository.WalletInfo
import com.example.p2meshandroid.data.storage.QueueStats
import com.example.p2meshandroid.data.storage.QueuedPayment
import com.example.p2meshandroid.data.storage.QueuedPaymentStatus
import com.example.p2meshandroid.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for wallet screen
 */
class WalletViewModel(
    private val loadWalletUseCase: LoadWalletUseCase,
    private val createWalletUseCase: CreateWalletUseCase,
    private val getWalletInfoUseCase: GetWalletInfoUseCase,
    private val sendPaymentUseCase: SendPaymentUseCase,
    private val receivePaymentUseCase: ReceivePaymentUseCase,
    private val getPendingPaymentsUseCase: GetPendingPaymentsUseCase,
    private val backupWalletUseCase: BackupWalletUseCase,
    private val fundFromFaucetUseCase: FundFromFaucetUseCase,
    private val restoreWalletUseCase: RestoreWalletUseCase? = null,
    // Payment Queue use cases
    private val queuePaymentUseCase: QueuePaymentUseCase? = null,
    private val processPaymentQueueUseCase: ProcessPaymentQueueUseCase? = null,
    private val getQueuedPaymentsUseCase: GetQueuedPaymentsUseCase? = null,
    private val getQueueStatsUseCase: GetQueueStatsUseCase? = null,
    private val retryQueuedPaymentUseCase: RetryQueuedPaymentUseCase? = null,
    private val cancelQueuedPaymentUseCase: CancelQueuedPaymentUseCase? = null,
    private val getQueueProcessingStateUseCase: GetQueueProcessingStateUseCase? = null,
    private val meshRepository: MeshRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _sendState = MutableStateFlow<SendPaymentState>(SendPaymentState.Idle)
    val sendState: StateFlow<SendPaymentState> = _sendState.asStateFlow()

    private val _receiveState = MutableStateFlow<ReceivePaymentState>(ReceivePaymentState.Idle)
    val receiveState: StateFlow<ReceivePaymentState> = _receiveState.asStateFlow()

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionItem>>(emptyList())
    val transactions: StateFlow<List<TransactionItem>> = _transactions.asStateFlow()

    // Payment Queue state
    private val _queuedPayments = MutableStateFlow<List<QueuedPayment>>(emptyList())
    val queuedPayments: StateFlow<List<QueuedPayment>> = _queuedPayments.asStateFlow()

    private val _queueStats = MutableStateFlow(QueueStats(0, 0, 0, 0, 0UL))
    val queueStats: StateFlow<QueueStats> = _queueStats.asStateFlow()

    private val _isProcessingQueue = MutableStateFlow(false)
    val isProcessingQueue: StateFlow<Boolean> = _isProcessingQueue.asStateFlow()

    init {
        loadOrCreateWallet()
        observeQueueState()
        startQueueAutoProcess()
    }

    private fun startQueueAutoProcess() {
        // Auto-process queued payments when connectivity is detected
        viewModelScope.launch {
            // Monitor connectivity and process queue when peers are available
            while (true) {
                kotlinx.coroutines.delay(5000)
                val hasConnectivity = meshRepository?.hasConnectedPeers() ?: false
                val hasPending = _queuedPayments.value.any {
                    it.status == QueuedPaymentStatus.PENDING
                }
                if (hasConnectivity && hasPending && !_isProcessingQueue.value) {
                    processQueue()
                }
            }
        }
    }

    private fun observeQueueState() {
        // Observe queued payments
        getQueuedPaymentsUseCase?.invoke()?.let { flow ->
            viewModelScope.launch {
                flow.collect { payments ->
                    _queuedPayments.value = payments
                }
            }
        }

        // Observe queue stats
        getQueueStatsUseCase?.invoke()?.let { flow ->
            viewModelScope.launch {
                flow.collect { stats ->
                    _queueStats.value = stats
                }
            }
        }

        // Observe processing state
        getQueueProcessingStateUseCase?.invoke()?.let { flow ->
            viewModelScope.launch {
                flow.collect { isProcessing ->
                    _isProcessingQueue.value = isProcessing
                }
            }
        }
    }

    private fun loadOrCreateWallet() {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading

            // First try to load from storage (persisted wallet)
            val loadResult = loadWalletUseCase()

            loadResult.onSuccess { walletInfo ->
                if (walletInfo != null) {
                    // Successfully loaded from storage
                    _uiState.value = WalletUiState.Success(walletInfo)
                    return@launch
                }
            }

            // No stored wallet or load failed - try in-memory wallet or create new
            val result = getWalletInfoUseCase()
                .recoverCatching { createWalletUseCase().getOrThrow() }

            _uiState.value = result.fold(
                onSuccess = { WalletUiState.Success(it) },
                onFailure = { WalletUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun createNewWallet() {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading

            val result = createWalletUseCase()
            _uiState.value = result.fold(
                onSuccess = { WalletUiState.Success(it) },
                onFailure = { WalletUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun refreshWallet() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is WalletUiState.Success) {
                val result = getWalletInfoUseCase()
                result.onSuccess {
                    _uiState.value = WalletUiState.Success(it)
                }
            }
        }
    }

    fun sendPayment(recipientDid: String, amount: ULong) {
        viewModelScope.launch {
            _sendState.value = SendPaymentState.Sending

            // Check if we have mesh connectivity
            val hasConnectivity = meshRepository?.hasConnectedPeers() ?: false

            if (!hasConnectivity && queuePaymentUseCase != null) {
                // No connectivity - queue the payment for later
                val queueResult = queuePaymentUseCase.invoke(recipientDid, amount)
                _sendState.value = queueResult.fold(
                    onSuccess = { queuedPayment ->
                        addTransaction(
                            TransactionItem(
                                id = queuedPayment.id,
                                type = TransactionType.SENT,
                                amount = amount,
                                counterparty = recipientDid,
                                timestamp = System.currentTimeMillis(),
                                status = TransactionStatus.PENDING
                            )
                        )
                        SendPaymentState.Queued(queuedPayment.id, amount)
                    },
                    onFailure = { SendPaymentState.Error(it.message ?: "Failed to queue payment") }
                )
            } else {
                // Has connectivity - send immediately
                val result = sendPaymentUseCase(recipientDid, amount)
                _sendState.value = result.fold(
                    onSuccess = { payment ->
                        // Add to transaction history
                        addTransaction(
                            TransactionItem(
                                id = payment.id,
                                type = TransactionType.SENT,
                                amount = payment.amount,
                                counterparty = payment.recipient,
                                timestamp = System.currentTimeMillis(),
                                status = TransactionStatus.COMPLETED
                            )
                        )
                        refreshWallet()
                        SendPaymentState.Success(payment.id, payment.amount)
                    },
                    onFailure = { SendPaymentState.Error(it.message ?: "Payment failed") }
                )
            }
        }
    }

    /**
     * Queue a payment for later delivery (explicit queue action)
     */
    fun queuePayment(recipientDid: String, amount: ULong) {
        viewModelScope.launch {
            _sendState.value = SendPaymentState.Sending

            val result = queuePaymentUseCase?.invoke(recipientDid, amount)
                ?: Result.failure(Exception("Queue not available"))

            _sendState.value = result.fold(
                onSuccess = { queuedPayment ->
                    addTransaction(
                        TransactionItem(
                            id = queuedPayment.id,
                            type = TransactionType.SENT,
                            amount = amount,
                            counterparty = recipientDid,
                            timestamp = System.currentTimeMillis(),
                            status = TransactionStatus.PENDING
                        )
                    )
                    SendPaymentState.Queued(queuedPayment.id, amount)
                },
                onFailure = { SendPaymentState.Error(it.message ?: "Failed to queue payment") }
            )
        }
    }

    /**
     * Process all queued payments
     */
    fun processQueue() {
        viewModelScope.launch {
            processPaymentQueueUseCase?.invoke()?.onSuccess { count ->
                if (count > 0) {
                    refreshWallet()
                    // Update transaction statuses
                    updateQueuedTransactionStatuses()
                }
            }
        }
    }

    /**
     * Retry a specific failed payment
     */
    fun retryQueuedPayment(paymentId: String) {
        viewModelScope.launch {
            retryQueuedPaymentUseCase?.invoke(paymentId)
            // Trigger queue processing
            processQueue()
        }
    }

    /**
     * Cancel a queued payment
     */
    fun cancelQueuedPayment(paymentId: String) {
        viewModelScope.launch {
            cancelQueuedPaymentUseCase?.invoke(paymentId)
            // Update transaction in history
            _transactions.value = _transactions.value.map { tx ->
                if (tx.id == paymentId) tx.copy(status = TransactionStatus.FAILED)
                else tx
            }
        }
    }

    private fun updateQueuedTransactionStatuses() {
        val queuedIds = _queuedPayments.value
            .filter { it.status == QueuedPaymentStatus.COMPLETED }
            .map { it.id }
            .toSet()

        _transactions.value = _transactions.value.map { tx ->
            if (tx.id in queuedIds && tx.status == TransactionStatus.PENDING) {
                tx.copy(status = TransactionStatus.COMPLETED)
            } else tx
        }
    }

    private fun addTransaction(transaction: TransactionItem) {
        _transactions.value = listOf(transaction) + _transactions.value
    }

    fun resetSendState() {
        _sendState.value = SendPaymentState.Idle
    }

    fun getBackup(onResult: (Result<WalletBackup>) -> Unit) {
        viewModelScope.launch {
            val result = backupWalletUseCase()
            onResult(result)
        }
    }

    fun fundFromFaucet(amount: ULong) {
        viewModelScope.launch {
            val result = fundFromFaucetUseCase(amount)
            result.onSuccess { walletInfo ->
                // Add to transaction history
                addTransaction(
                    TransactionItem(
                        id = "faucet_${System.currentTimeMillis()}",
                        type = TransactionType.RECEIVED,
                        amount = amount,
                        counterparty = "Faucet",
                        timestamp = System.currentTimeMillis(),
                        status = TransactionStatus.COMPLETED
                    )
                )
                _uiState.value = WalletUiState.Success(walletInfo)
            }
            result.onFailure { error ->
                // Keep current state but could show a snackbar/toast
            }
        }
    }

    /**
     * Process a received payment from hex-encoded IOU data
     */
    fun receivePayment(paymentHex: String) {
        viewModelScope.launch {
            _receiveState.value = ReceivePaymentState.Processing

            try {
                // Convert hex string to bytes
                val paymentBytes = paymentHex.chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray()

                val result = receivePaymentUseCase(paymentBytes)
                _receiveState.value = result.fold(
                    onSuccess = {
                        // Add to transaction history
                        addTransaction(
                            TransactionItem(
                                id = "recv_${System.currentTimeMillis()}",
                                type = TransactionType.RECEIVED,
                                amount = 0UL, // Amount unknown without parsing IOU
                                counterparty = "Unknown",
                                timestamp = System.currentTimeMillis(),
                                status = TransactionStatus.COMPLETED
                            )
                        )
                        refreshWallet()
                        ReceivePaymentState.Success
                    },
                    onFailure = { ReceivePaymentState.Error(it.message ?: "Failed to process payment") }
                )
            } catch (e: Exception) {
                _receiveState.value = ReceivePaymentState.Error("Invalid payment data: ${e.message}")
            }
        }
    }

    /**
     * Process a received payment with explicit sender public key
     */
    fun receivePaymentWithKey(paymentHex: String, senderPubKeyHex: String) {
        viewModelScope.launch {
            _receiveState.value = ReceivePaymentState.Processing

            try {
                val paymentBytes = paymentHex.chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray()

                val senderPubKey = senderPubKeyHex.chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray()

                val result = receivePaymentUseCase.withSenderKey(paymentBytes, senderPubKey)
                _receiveState.value = result.fold(
                    onSuccess = {
                        // Add to transaction history
                        addTransaction(
                            TransactionItem(
                                id = "recv_${System.currentTimeMillis()}",
                                type = TransactionType.RECEIVED,
                                amount = 0UL,
                                counterparty = "Unknown",
                                timestamp = System.currentTimeMillis(),
                                status = TransactionStatus.COMPLETED
                            )
                        )
                        refreshWallet()
                        ReceivePaymentState.Success
                    },
                    onFailure = { ReceivePaymentState.Error(it.message ?: "Failed to process payment") }
                )
            } catch (e: Exception) {
                _receiveState.value = ReceivePaymentState.Error("Invalid data: ${e.message}")
            }
        }
    }

    fun resetReceiveState() {
        _receiveState.value = ReceivePaymentState.Idle
    }

    /**
     * Restore wallet from hex-encoded secret key
     */
    fun restoreWallet(secretKeyHex: String) {
        viewModelScope.launch {
            _restoreState.value = RestoreState.Restoring

            try {
                // Convert hex string to bytes
                val secretKeyBytes = secretKeyHex.trim()
                    .chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray()

                val result = restoreWalletUseCase?.invoke(secretKeyBytes)
                    ?: Result.failure(Exception("Restore not available"))

                result.fold(
                    onSuccess = { walletInfo ->
                        _uiState.value = WalletUiState.Success(walletInfo)
                        _restoreState.value = RestoreState.Success
                        // Clear transaction history for restored wallet
                        _transactions.value = emptyList()
                    },
                    onFailure = { error ->
                        _restoreState.value = RestoreState.Error(error.message ?: "Restore failed")
                    }
                )
            } catch (e: Exception) {
                _restoreState.value = RestoreState.Error("Invalid secret key format: ${e.message}")
            }
        }
    }

    fun resetRestoreState() {
        _restoreState.value = RestoreState.Idle
    }
}

/**
 * UI state for wallet screen
 */
sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Success(val walletInfo: WalletInfo) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}

/**
 * State for send payment operation
 */
sealed class SendPaymentState {
    object Idle : SendPaymentState()
    object Sending : SendPaymentState()
    data class Success(val paymentId: String, val amount: ULong) : SendPaymentState()
    data class Queued(val paymentId: String, val amount: ULong) : SendPaymentState()
    data class Error(val message: String) : SendPaymentState()
}

/**
 * State for receive payment operation
 */
sealed class ReceivePaymentState {
    object Idle : ReceivePaymentState()
    object Processing : ReceivePaymentState()
    object Success : ReceivePaymentState()
    data class Error(val message: String) : ReceivePaymentState()
}

/**
 * State for wallet restore operation
 */
sealed class RestoreState {
    object Idle : RestoreState()
    object Restoring : RestoreState()
    object Success : RestoreState()
    data class Error(val message: String) : RestoreState()
}

/**
 * Transaction type
 */
enum class TransactionType {
    SENT,
    RECEIVED
}

/**
 * Transaction status
 */
enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}

/**
 * Transaction item for history display
 */
data class TransactionItem(
    val id: String,
    val type: TransactionType,
    val amount: ULong,
    val counterparty: String,
    val timestamp: Long,
    val status: TransactionStatus
) {
    val shortCounterparty: String
        get() = if (counterparty.length > 20) "${counterparty.take(10)}...${counterparty.takeLast(6)}" else counterparty

    val formattedTime: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                else -> "${diff / 86400_000}d ago"
            }
        }
}
