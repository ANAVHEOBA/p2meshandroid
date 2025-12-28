package com.example.p2meshandroid.presentation.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.p2meshandroid.di.SettlementViewModelFactory
import com.example.p2meshandroid.ui.theme.*

@Composable
fun SettlementScreen(
    settlementViewModel: SettlementViewModel = viewModel(factory = SettlementViewModelFactory()),
    modifier: Modifier = Modifier
) {
    val uiState by settlementViewModel.uiState.collectAsState()
    val operationState by settlementViewModel.operationState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Settlement",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Batch and settle IOUs on-chain",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Operation Status
        item {
            OperationStatusCard(operationState, settlementViewModel::resetOperationState)
        }

        when (val state = uiState) {
            is SettlementUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }
                }
            }

            is SettlementUiState.Error -> {
                item {
                    ErrorCard(state.message)
                }
            }

            is SettlementUiState.Ready -> {
                // Stats Card
                item {
                    StatsCard(state)
                }

                // Current Batch Card
                if (state.currentBatch != null) {
                    item {
                        CurrentBatchCard(
                            batch = state.currentBatch,
                            onSubmit = { settlementViewModel.submitBatch() },
                            onSimulate = { settlementViewModel.simulateSettlement(state.currentBatch.id) }
                        )
                    }
                }

                // Action Buttons
                item {
                    ActionButtons(
                        hasCurrentBatch = state.currentBatch != null,
                        onCollect = { settlementViewModel.collectFromWallet() },
                        onCreateBatch = { settlementViewModel.createBatch() },
                        onClear = { settlementViewModel.clearAll() }
                    )
                }

                // Results Section
                if (state.results.isNotEmpty()) {
                    item {
                        Text(
                            text = "Settlement History",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(state.results) { result ->
                        ResultCard(result)
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationStatusCard(
    state: OperationState,
    onDismiss: () -> Unit
) {
    when (state) {
        is OperationState.Idle -> {}
        is OperationState.Collecting -> StatusCard("Collecting IOUs...", PrimaryPurple, true)
        is OperationState.CollectSuccess -> StatusCard("Collected ${state.count} IOUs", SuccessGreen, false, onDismiss)
        is OperationState.CreatingBatch -> StatusCard("Creating batch...", PrimaryPurple, true)
        is OperationState.BatchCreated -> StatusCard("Batch created: ${state.batch.shortId}", SuccessGreen, false, onDismiss)
        is OperationState.Submitting -> StatusCard("Submitting batch...", PrimaryPurple, true)
        is OperationState.Submitted -> StatusCard("Batch submitted!", SuccessGreen, false, onDismiss)
        is OperationState.Simulating -> StatusCard("Simulating settlement...", PrimaryPurple, true)
        is OperationState.SimulationComplete -> StatusCard(
            if (state.success) "Settlement successful!" else "Settlement failed",
            if (state.success) SuccessGreen else ErrorRed,
            false,
            onDismiss
        )
        is OperationState.Error -> StatusCard(state.message, ErrorRed, false, onDismiss)
    }
}

@Composable
private fun StatusCard(
    message: String,
    color: androidx.compose.ui.graphics.Color,
    showProgress: Boolean,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (color == SuccessGreen) Icons.Filled.CheckCircle else Icons.Filled.Info,
                    contentDescription = null,
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = color,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = color
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(state: SettlementUiState.Ready) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Settlement Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Manage IOU batches",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Collected", state.totalCollected.toString())
                StatItem("Pending", state.pendingBatches.toString())
                StatItem("Settled", state.successfulSettlements.toString())
                StatItem("Failed", state.failedSettlements.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun CurrentBatchCard(
    batch: com.example.p2meshandroid.data.repository.BatchInfo,
    onSubmit: () -> Unit,
    onSimulate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Inventory,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Current Batch",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryPurple,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Batch details
            DetailRow("Batch ID", batch.shortId)
            DetailRow("Entries", batch.entryCount.toString())
            DetailRow("Total Amount", "${batch.totalAmount} credits")
            DetailRow("Status", batch.status)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Simulate")
                }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Icon(Icons.Filled.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ActionButtons(
    hasCurrentBatch: Boolean,
    onCollect: () -> Unit,
    onCreateBatch: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCollect,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Collect IOUs")
            }
            Button(
                onClick = onCreateBatch,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryPurple),
                enabled = !hasCurrentBatch
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Batch")
            }
        }

        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Clear All Data")
        }
    }
}

@Composable
private fun ResultCard(result: com.example.p2meshandroid.data.repository.SettlementResultInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (result.success) SuccessGreen.copy(alpha = 0.2f)
                        else ErrorRed.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (result.success) SuccessGreen else ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.shortBatchId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (result.success) {
                        result.transactionId?.let { "TX: ${it.take(16)}..." } ?: "Settled"
                    } else {
                        result.errorMessage ?: "Failed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "x${result.attempts}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = ErrorRed
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = message, color = ErrorRed)
        }
    }
}
