package com.example.p2meshandroid.data.repository

import android.util.Log
import com.example.p2meshandroid.data.storage.PaymentQueueStorage
import com.example.p2meshandroid.data.storage.QueueStats
import com.example.p2meshandroid.data.storage.QueuedPayment
import com.example.p2meshandroid.data.storage.QueuedPaymentStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing offline payment queue.
 * Handles queuing payments when offline and auto-sending when connectivity is restored.
 */
class PaymentQueueRepository(
    private val storage: PaymentQueueStorage,
    private val walletRepository: WalletRepository,
    private val meshRepository: MeshRepository
) {
    companion object {
        private const val TAG = "PaymentQueueRepository"
        private const val PROCESS_DELAY_MS = 1000L // Delay between processing payments
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _queuedPayments = MutableStateFlow<List<QueuedPayment>>(emptyList())
    val queuedPayments: StateFlow<List<QueuedPayment>> = _queuedPayments.asStateFlow()

    private val _queueStats = MutableStateFlow(QueueStats(0, 0, 0, 0, 0UL))
    val queueStats: StateFlow<QueueStats> = _queueStats.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var autoProcessJob: Job? = null

    init {
        refreshQueue()
    }

    /**
     * Queue a payment for later delivery
     */
    suspend fun queuePayment(recipientDid: String, amount: ULong): Result<QueuedPayment> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payment = storage.enqueue(recipientDid, amount)
                Log.d(TAG, "Payment queued: ${payment.id} to $recipientDid for $amount")
                refreshQueue()
                payment
            }
        }

    /**
     * Process all pending payments
     * Returns the number of successfully sent payments
     */
    suspend fun processQueue(): Result<Int> = withContext(Dispatchers.IO) {
        if (_isProcessing.value) {
            return@withContext Result.failure(IllegalStateException("Already processing queue"))
        }

        runCatching {
            _isProcessing.value = true
            var successCount = 0

            val pending = storage.getPending()
            Log.d(TAG, "Processing ${pending.size} pending payments")

            for (payment in pending) {
                try {
                    // Mark as sending
                    storage.markSending(payment.id)
                    refreshQueue()

                    // Try to send
                    val result = sendPayment(payment)
                    if (result.isSuccess) {
                        storage.markCompleted(payment.id)
                        successCount++
                        Log.d(TAG, "Payment ${payment.id} sent successfully")
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        storage.markFailed(payment.id, error)
                        Log.e(TAG, "Payment ${payment.id} failed: $error")
                    }
                    refreshQueue()

                    // Small delay between payments
                    delay(PROCESS_DELAY_MS)
                } catch (e: Exception) {
                    storage.markFailed(payment.id, e.message ?: "Unknown error")
                    Log.e(TAG, "Payment ${payment.id} failed with exception", e)
                    refreshQueue()
                }
            }

            _isProcessing.value = false
            successCount
        }.also {
            _isProcessing.value = false
        }
    }

    /**
     * Retry a specific failed payment
     */
    suspend fun retryPayment(paymentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            storage.resetForRetry(paymentId)
            refreshQueue()
            Log.d(TAG, "Payment $paymentId reset for retry")
            Unit
        }
    }

    /**
     * Cancel/remove a queued payment
     */
    suspend fun cancelPayment(paymentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            storage.remove(paymentId)
            refreshQueue()
            Log.d(TAG, "Payment $paymentId cancelled")
            Unit
        }
    }

    /**
     * Get all queued payments
     */
    fun getQueuedPayments(): List<QueuedPayment> = storage.getAll()

    /**
     * Get queue statistics
     */
    fun getStats(): QueueStats = storage.getStats()

    /**
     * Clear all queued payments
     */
    suspend fun clearQueue(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            storage.clearAll()
            refreshQueue()
            Log.d(TAG, "Queue cleared")
            Unit
        }
    }

    /**
     * Start auto-processing when connectivity is available
     */
    fun startAutoProcess() {
        autoProcessJob?.cancel()
        autoProcessJob = scope.launch {
            while (isActive) {
                // Check if we have connectivity (connected peers)
                val connectedPeers = meshRepository.getConnectedPeers().getOrNull() ?: emptyList()
                val pendingCount = storage.getPending().size

                if (connectedPeers.isNotEmpty() && pendingCount > 0 && !_isProcessing.value) {
                    Log.d(TAG, "Auto-processing queue: ${connectedPeers.size} peers, $pendingCount pending")
                    processQueue()
                }

                delay(5000) // Check every 5 seconds
            }
        }
    }

    /**
     * Stop auto-processing
     */
    fun stopAutoProcess() {
        autoProcessJob?.cancel()
        autoProcessJob = null
    }

    /**
     * Check if there are pending payments in queue
     */
    fun hasPendingPayments(): Boolean = storage.getPending().isNotEmpty()

    /**
     * Notify that connectivity was restored - trigger queue processing
     */
    suspend fun onConnectivityRestored() {
        Log.d(TAG, "Connectivity restored, processing queue")
        if (hasPendingPayments() && !_isProcessing.value) {
            processQueue()
        }
    }

    private suspend fun sendPayment(payment: QueuedPayment): Result<Unit> {
        // Create the payment IOU
        val paymentResult = walletRepository.createPayment(
            recipientDid = payment.recipientDid,
            amount = payment.amount
        )

        if (paymentResult.isFailure) {
            return Result.failure(
                paymentResult.exceptionOrNull() ?: Exception("Failed to create payment")
            )
        }

        val paymentInfo = paymentResult.getOrThrow()

        // Broadcast via mesh network
        val broadcastResult = meshRepository.broadcastPayment(paymentInfo.iou)
        if (broadcastResult.isFailure) {
            return Result.failure(
                broadcastResult.exceptionOrNull() ?: Exception("Failed to broadcast payment")
            )
        }

        // Mark as sent in wallet
        walletRepository.markPaymentSent(paymentInfo.iou)

        return Result.success(Unit)
    }

    private fun refreshQueue() {
        _queuedPayments.value = storage.getAll()
        _queueStats.value = storage.getStats()
    }
}
