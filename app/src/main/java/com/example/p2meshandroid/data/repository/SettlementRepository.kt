package com.example.p2meshandroid.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.p2pmesh_bridge.*

/**
 * Repository for settlement operations.
 * Handles collecting IOUs into batches and submitting for settlement.
 */
class SettlementRepository(
    private val walletRepository: WalletRepository
) {
    private var collector: Collector? = null
    private var settler: Settler? = null

    /**
     * Initialize collector with default config
     */
    suspend fun initializeCollector(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (collector == null) {
                collector = Collector()
            }
        }
    }

    /**
     * Initialize collector with custom config
     */
    suspend fun initializeCollectorWithConfig(
        minBatchSize: UInt,
        maxBatchSize: UInt,
        minAmount: ULong
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            collector = Collector.withConfig(minBatchSize, maxBatchSize, minAmount)
        }
    }

    /**
     * Initialize settler with default config
     */
    suspend fun initializeSettler(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (settler == null) {
                settler = Settler()
            }
        }
    }

    /**
     * Initialize settler with custom config
     */
    suspend fun initializeSettlerWithConfig(
        maxRetries: UInt,
        timeoutSecs: ULong
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            settler = Settler.withConfig(maxRetries, timeoutSecs)
        }
    }

    /**
     * Collect IOUs from wallet into collector
     */
    suspend fun collectFromWallet(wallet: Wallet): Result<ULong> = withContext(Dispatchers.IO) {
        runCatching {
            val c = collector ?: throw IllegalStateException("Collector not initialized")
            c.collectFromWallet(wallet)
        }
    }

    /**
     * Create a settlement batch from collected IOUs
     */
    suspend fun createBatch(): Result<BatchInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val c = collector ?: throw IllegalStateException("Collector not initialized")
            val batch = c.createBatch()
            BatchInfo(
                id = batch.id(),
                entryCount = batch.entryCount(),
                totalAmount = batch.totalAmount(),
                status = batch.status(),
                batch = batch
            )
        }
    }

    /**
     * Get collector statistics
     */
    suspend fun getCollectorStats(): Result<CollectorStats> = withContext(Dispatchers.IO) {
        runCatching {
            val c = collector ?: return@runCatching CollectorStats(0UL, 0UL)
            CollectorStats(
                totalCollected = c.totalCollected(),
                pendingBatches = c.pendingBatches()
            )
        }
    }

    /**
     * Clear all batches from collector
     */
    suspend fun clearCollector(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            collector?.clear() ?: Unit
        }
    }

    /**
     * Submit a batch for settlement
     */
    suspend fun submitBatch(batch: SettlementBatch): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settler ?: throw IllegalStateException("Settler not initialized")
            s.submit(batch)
        }
    }

    /**
     * Get settlement results
     */
    suspend fun getSettlementResults(): Result<List<SettlementResultInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settler ?: return@runCatching emptyList()
            s.results().map { result ->
                SettlementResultInfo(
                    success = result.success,
                    batchId = result.batchId,
                    transactionId = result.transactionId,
                    errorMessage = result.errorMessage,
                    attempts = result.attempts
                )
            }
        }
    }

    /**
     * Get pending settlement count
     */
    suspend fun getPendingSettlementCount(): Result<ULong> = withContext(Dispatchers.IO) {
        runCatching {
            settler?.pendingCount() ?: 0UL
        }
    }

    /**
     * Simulate processing a batch (for demo/testing)
     */
    suspend fun simulateSettlement(
        batchId: String,
        success: Boolean,
        txId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settler ?: throw IllegalStateException("Settler not initialized")
            s.simulateProcess(batchId, success, txId)
        }
    }

    /**
     * Clear settlement results
     */
    suspend fun clearSettlementResults(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            settler?.clearResults() ?: Unit
        }
    }

    fun isCollectorInitialized(): Boolean = collector != null
    fun isSettlerInitialized(): Boolean = settler != null
}

/**
 * Info about a settlement batch
 */
data class BatchInfo(
    val id: String,
    val entryCount: ULong,
    val totalAmount: ULong,
    val status: String,
    val batch: SettlementBatch
) {
    val shortId: String
        get() = if (id.length > 16) "${id.take(8)}...${id.takeLast(8)}" else id
}

/**
 * Collector statistics
 */
data class CollectorStats(
    val totalCollected: ULong,
    val pendingBatches: ULong
)

/**
 * Settlement result info
 */
data class SettlementResultInfo(
    val success: Boolean,
    val batchId: String,
    val transactionId: String?,
    val errorMessage: String?,
    val attempts: UInt
) {
    val shortBatchId: String
        get() = if (batchId.length > 16) "${batchId.take(8)}...${batchId.takeLast(8)}" else batchId
}
