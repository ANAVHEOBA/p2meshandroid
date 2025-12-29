package com.example.p2meshandroid.data.storage

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Represents a payment waiting to be sent when connectivity is restored
 */
@Serializable
data class QueuedPayment(
    val id: String = UUID.randomUUID().toString(),
    val recipientDid: String,
    val amount: ULong,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null,
    val status: QueuedPaymentStatus = QueuedPaymentStatus.PENDING
)

@Serializable
enum class QueuedPaymentStatus {
    PENDING,      // Waiting to be sent
    SENDING,      // Currently being processed
    FAILED,       // Failed after retries (can be retried manually)
    COMPLETED     // Successfully sent (will be removed)
}

/**
 * Handles persistent storage for queued payments.
 * Uses SharedPreferences with JSON serialization.
 */
class PaymentQueueStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "payment_queue_prefs"
        private const val KEY_QUEUED_PAYMENTS = "queued_payments"
        private const val MAX_RETRY_COUNT = 3
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Add a payment to the queue
     */
    fun enqueue(recipientDid: String, amount: ULong): QueuedPayment {
        val payment = QueuedPayment(
            recipientDid = recipientDid,
            amount = amount
        )

        val payments = getAll().toMutableList()
        payments.add(payment)
        saveAll(payments)

        return payment
    }

    /**
     * Get all queued payments
     */
    fun getAll(): List<QueuedPayment> {
        val jsonString = prefs.getString(KEY_QUEUED_PAYMENTS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<QueuedPayment>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get pending payments (not completed or being sent)
     */
    fun getPending(): List<QueuedPayment> {
        return getAll().filter { it.status == QueuedPaymentStatus.PENDING }
    }

    /**
     * Get failed payments (can be retried)
     */
    fun getFailed(): List<QueuedPayment> {
        return getAll().filter { it.status == QueuedPaymentStatus.FAILED }
    }

    /**
     * Update a queued payment
     */
    fun update(payment: QueuedPayment) {
        val payments = getAll().toMutableList()
        val index = payments.indexOfFirst { it.id == payment.id }
        if (index >= 0) {
            payments[index] = payment
            saveAll(payments)
        }
    }

    /**
     * Mark payment as sending
     */
    fun markSending(paymentId: String) {
        val payments = getAll().toMutableList()
        val index = payments.indexOfFirst { it.id == paymentId }
        if (index >= 0) {
            payments[index] = payments[index].copy(status = QueuedPaymentStatus.SENDING)
            saveAll(payments)
        }
    }

    /**
     * Mark payment as completed and remove it
     */
    fun markCompleted(paymentId: String) {
        val payments = getAll().toMutableList()
        payments.removeAll { it.id == paymentId }
        saveAll(payments)
    }

    /**
     * Mark payment as failed with error
     */
    fun markFailed(paymentId: String, error: String) {
        val payments = getAll().toMutableList()
        val index = payments.indexOfFirst { it.id == paymentId }
        if (index >= 0) {
            val current = payments[index]
            val newRetryCount = current.retryCount + 1
            payments[index] = current.copy(
                status = if (newRetryCount >= MAX_RETRY_COUNT)
                    QueuedPaymentStatus.FAILED
                else
                    QueuedPaymentStatus.PENDING,
                retryCount = newRetryCount,
                lastError = error
            )
            saveAll(payments)
        }
    }

    /**
     * Reset a failed payment to pending for retry
     */
    fun resetForRetry(paymentId: String) {
        val payments = getAll().toMutableList()
        val index = payments.indexOfFirst { it.id == paymentId }
        if (index >= 0) {
            payments[index] = payments[index].copy(
                status = QueuedPaymentStatus.PENDING,
                lastError = null
            )
            saveAll(payments)
        }
    }

    /**
     * Remove a payment from queue
     */
    fun remove(paymentId: String) {
        val payments = getAll().toMutableList()
        payments.removeAll { it.id == paymentId }
        saveAll(payments)
    }

    /**
     * Clear all queued payments
     */
    fun clearAll() {
        prefs.edit().remove(KEY_QUEUED_PAYMENTS).apply()
    }

    /**
     * Get queue statistics
     */
    fun getStats(): QueueStats {
        val all = getAll()
        return QueueStats(
            totalCount = all.size,
            pendingCount = all.count { it.status == QueuedPaymentStatus.PENDING },
            sendingCount = all.count { it.status == QueuedPaymentStatus.SENDING },
            failedCount = all.count { it.status == QueuedPaymentStatus.FAILED },
            totalAmount = all.filter { it.status != QueuedPaymentStatus.COMPLETED }
                .sumOf { it.amount.toLong() }.toULong()
        )
    }

    private fun saveAll(payments: List<QueuedPayment>) {
        val jsonString = json.encodeToString(payments)
        prefs.edit().putString(KEY_QUEUED_PAYMENTS, jsonString).apply()
    }
}

/**
 * Statistics about the payment queue
 */
data class QueueStats(
    val totalCount: Int,
    val pendingCount: Int,
    val sendingCount: Int,
    val failedCount: Int,
    val totalAmount: ULong
)
