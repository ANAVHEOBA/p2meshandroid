package com.example.p2meshandroid.domain.usecase

import com.example.p2meshandroid.data.repository.PaymentQueueRepository
import com.example.p2meshandroid.data.storage.QueueStats
import com.example.p2meshandroid.data.storage.QueuedPayment
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for queuing a payment when offline
 */
class QueuePaymentUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    suspend operator fun invoke(recipientDid: String, amount: ULong): Result<QueuedPayment> {
        return paymentQueueRepository.queuePayment(recipientDid, amount)
    }
}

/**
 * Use case for processing the payment queue
 */
class ProcessPaymentQueueUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    suspend operator fun invoke(): Result<Int> {
        return paymentQueueRepository.processQueue()
    }
}

/**
 * Use case for getting queued payments
 */
class GetQueuedPaymentsUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    operator fun invoke(): StateFlow<List<QueuedPayment>> {
        return paymentQueueRepository.queuedPayments
    }
}

/**
 * Use case for getting queue statistics
 */
class GetQueueStatsUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    operator fun invoke(): StateFlow<QueueStats> {
        return paymentQueueRepository.queueStats
    }
}

/**
 * Use case for retrying a failed payment
 */
class RetryQueuedPaymentUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    suspend operator fun invoke(paymentId: String): Result<Unit> {
        return paymentQueueRepository.retryPayment(paymentId)
    }
}

/**
 * Use case for cancelling a queued payment
 */
class CancelQueuedPaymentUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    suspend operator fun invoke(paymentId: String): Result<Unit> {
        return paymentQueueRepository.cancelPayment(paymentId)
    }
}

/**
 * Use case for clearing the payment queue
 */
class ClearPaymentQueueUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return paymentQueueRepository.clearQueue()
    }
}

/**
 * Use case for starting auto-process when connectivity is available
 */
class StartQueueAutoProcessUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    operator fun invoke() {
        paymentQueueRepository.startAutoProcess()
    }
}

/**
 * Use case for stopping auto-process
 */
class StopQueueAutoProcessUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    operator fun invoke() {
        paymentQueueRepository.stopAutoProcess()
    }
}

/**
 * Use case for checking if there are pending payments
 */
class HasPendingQueuedPaymentsUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    operator fun invoke(): Boolean {
        return paymentQueueRepository.hasPendingPayments()
    }
}

/**
 * Use case for notifying connectivity restored
 */
class NotifyConnectivityRestoredUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    suspend operator fun invoke() {
        paymentQueueRepository.onConnectivityRestored()
    }
}

/**
 * Use case for getting queue processing state
 */
class GetQueueProcessingStateUseCase(
    private val paymentQueueRepository: PaymentQueueRepository
) {
    operator fun invoke(): StateFlow<Boolean> {
        return paymentQueueRepository.isProcessing
    }
}
