package com.example.p2meshandroid.domain.usecase

import com.example.p2meshandroid.data.repository.*
import uniffi.p2pmesh_bridge.Wallet

/**
 * Use case for initializing settlement components
 */
class InitializeSettlementUseCase(
    private val settlementRepository: SettlementRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val collectorResult = settlementRepository.initializeCollector()
        if (collectorResult.isFailure) return collectorResult

        return settlementRepository.initializeSettler()
    }
}

/**
 * Use case for collecting IOUs from wallet
 */
class CollectIousUseCase(
    private val settlementRepository: SettlementRepository
) {
    suspend operator fun invoke(wallet: Wallet): Result<ULong> {
        // Ensure collector is initialized
        if (!settlementRepository.isCollectorInitialized()) {
            settlementRepository.initializeCollector()
        }
        return settlementRepository.collectFromWallet(wallet)
    }
}

/**
 * Use case for creating a settlement batch
 */
class CreateSettlementBatchUseCase(
    private val settlementRepository: SettlementRepository
) {
    suspend operator fun invoke(): Result<BatchInfo> {
        return settlementRepository.createBatch()
    }
}

/**
 * Use case for submitting a batch for settlement
 */
class SubmitSettlementBatchUseCase(
    private val settlementRepository: SettlementRepository
) {
    suspend operator fun invoke(batchInfo: BatchInfo): Result<Unit> {
        // Ensure settler is initialized
        if (!settlementRepository.isSettlerInitialized()) {
            settlementRepository.initializeSettler()
        }
        return settlementRepository.submitBatch(batchInfo.batch)
    }
}

/**
 * Use case for getting settlement statistics
 */
class GetSettlementStatsUseCase(
    private val settlementRepository: SettlementRepository
) {
    suspend operator fun invoke(): Result<SettlementStats> {
        val collectorStats = settlementRepository.getCollectorStats().getOrNull()
        val pendingCount = settlementRepository.getPendingSettlementCount().getOrNull() ?: 0UL
        val results = settlementRepository.getSettlementResults().getOrNull() ?: emptyList()

        val successCount = results.count { it.success }
        val failedCount = results.count { !it.success }

        return Result.success(
            SettlementStats(
                totalCollected = collectorStats?.totalCollected ?: 0UL,
                pendingBatches = collectorStats?.pendingBatches ?: 0UL,
                pendingSettlements = pendingCount,
                successfulSettlements = successCount,
                failedSettlements = failedCount,
                results = results
            )
        )
    }
}

/**
 * Use case for simulating settlement (demo mode)
 */
class SimulateSettlementUseCase(
    private val settlementRepository: SettlementRepository
) {
    suspend operator fun invoke(
        batchId: String,
        success: Boolean = true,
        txId: String? = "demo_tx_${System.currentTimeMillis()}"
    ): Result<Unit> {
        return settlementRepository.simulateSettlement(batchId, success, txId)
    }
}

/**
 * Use case for clearing settlement data
 */
class ClearSettlementDataUseCase(
    private val settlementRepository: SettlementRepository
) {
    suspend fun clearCollector(): Result<Unit> {
        return settlementRepository.clearCollector()
    }

    suspend fun clearResults(): Result<Unit> {
        return settlementRepository.clearSettlementResults()
    }

    suspend fun clearAll(): Result<Unit> {
        settlementRepository.clearCollector()
        return settlementRepository.clearSettlementResults()
    }
}

/**
 * Combined settlement statistics
 */
data class SettlementStats(
    val totalCollected: ULong,
    val pendingBatches: ULong,
    val pendingSettlements: ULong,
    val successfulSettlements: Int,
    val failedSettlements: Int,
    val results: List<SettlementResultInfo>
)
