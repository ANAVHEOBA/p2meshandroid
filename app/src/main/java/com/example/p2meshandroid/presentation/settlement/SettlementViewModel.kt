package com.example.p2meshandroid.presentation.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2meshandroid.data.repository.BatchInfo
import com.example.p2meshandroid.data.repository.SettlementResultInfo
import com.example.p2meshandroid.data.repository.WalletRepository
import com.example.p2meshandroid.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for settlement screen
 */
class SettlementViewModel(
    private val walletRepository: WalletRepository,
    private val initializeSettlementUseCase: InitializeSettlementUseCase,
    private val collectIousUseCase: CollectIousUseCase,
    private val createSettlementBatchUseCase: CreateSettlementBatchUseCase,
    private val submitSettlementBatchUseCase: SubmitSettlementBatchUseCase,
    private val getSettlementStatsUseCase: GetSettlementStatsUseCase,
    private val simulateSettlementUseCase: SimulateSettlementUseCase,
    private val clearSettlementDataUseCase: ClearSettlementDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettlementUiState>(SettlementUiState.Loading)
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private var currentBatch: BatchInfo? = null

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            _uiState.value = SettlementUiState.Loading

            val initResult = initializeSettlementUseCase()
            if (initResult.isFailure) {
                _uiState.value = SettlementUiState.Error(
                    initResult.exceptionOrNull()?.message ?: "Failed to initialize"
                )
                return@launch
            }

            refreshStats()
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            val statsResult = getSettlementStatsUseCase()
            statsResult.fold(
                onSuccess = { stats ->
                    _uiState.value = SettlementUiState.Ready(
                        totalCollected = stats.totalCollected,
                        pendingBatches = stats.pendingBatches,
                        pendingSettlements = stats.pendingSettlements,
                        successfulSettlements = stats.successfulSettlements,
                        failedSettlements = stats.failedSettlements,
                        results = stats.results,
                        currentBatch = currentBatch
                    )
                },
                onFailure = { error ->
                    _uiState.value = SettlementUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    /**
     * Collect IOUs from wallet
     */
    fun collectFromWallet() {
        viewModelScope.launch {
            _operationState.value = OperationState.Collecting

            // Get the native wallet from repository
            val walletResult = walletRepository.getNativeWallet()
            if (walletResult.isFailure) {
                _operationState.value = OperationState.Error("No wallet available")
                return@launch
            }

            val wallet = walletResult.getOrNull()!!
            val collectResult = collectIousUseCase(wallet)

            collectResult.fold(
                onSuccess = { count ->
                    _operationState.value = OperationState.CollectSuccess(count)
                    refreshStats()
                },
                onFailure = { error ->
                    _operationState.value = OperationState.Error(error.message ?: "Collection failed")
                }
            )
        }
    }

    /**
     * Create a batch from collected IOUs
     */
    fun createBatch() {
        viewModelScope.launch {
            _operationState.value = OperationState.CreatingBatch

            val batchResult = createSettlementBatchUseCase()
            batchResult.fold(
                onSuccess = { batch ->
                    currentBatch = batch
                    _operationState.value = OperationState.BatchCreated(batch)
                    refreshStats()
                },
                onFailure = { error ->
                    _operationState.value = OperationState.Error(error.message ?: "Batch creation failed")
                }
            )
        }
    }

    /**
     * Submit current batch for settlement
     */
    fun submitBatch() {
        val batch = currentBatch ?: return

        viewModelScope.launch {
            _operationState.value = OperationState.Submitting

            val submitResult = submitSettlementBatchUseCase(batch)
            submitResult.fold(
                onSuccess = {
                    _operationState.value = OperationState.Submitted(batch.id)
                    refreshStats()
                },
                onFailure = { error ->
                    _operationState.value = OperationState.Error(error.message ?: "Submission failed")
                }
            )
        }
    }

    /**
     * Simulate settlement (for demo)
     */
    fun simulateSettlement(batchId: String, success: Boolean = true) {
        viewModelScope.launch {
            _operationState.value = OperationState.Simulating

            val result = simulateSettlementUseCase(batchId, success)
            result.fold(
                onSuccess = {
                    _operationState.value = OperationState.SimulationComplete(success)
                    currentBatch = null
                    refreshStats()
                },
                onFailure = { error ->
                    _operationState.value = OperationState.Error(error.message ?: "Simulation failed")
                }
            )
        }
    }

    /**
     * Clear all data
     */
    fun clearAll() {
        viewModelScope.launch {
            clearSettlementDataUseCase.clearAll()
            currentBatch = null
            refreshStats()
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}

/**
 * UI state for settlement screen
 */
sealed class SettlementUiState {
    object Loading : SettlementUiState()

    data class Ready(
        val totalCollected: ULong,
        val pendingBatches: ULong,
        val pendingSettlements: ULong,
        val successfulSettlements: Int,
        val failedSettlements: Int,
        val results: List<SettlementResultInfo>,
        val currentBatch: BatchInfo?
    ) : SettlementUiState()

    data class Error(val message: String) : SettlementUiState()
}

/**
 * Operation state for settlement actions
 */
sealed class OperationState {
    object Idle : OperationState()
    object Collecting : OperationState()
    data class CollectSuccess(val count: ULong) : OperationState()
    object CreatingBatch : OperationState()
    data class BatchCreated(val batch: BatchInfo) : OperationState()
    object Submitting : OperationState()
    data class Submitted(val batchId: String) : OperationState()
    object Simulating : OperationState()
    data class SimulationComplete(val success: Boolean) : OperationState()
    data class Error(val message: String) : OperationState()
}
