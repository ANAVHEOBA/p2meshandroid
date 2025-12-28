package com.example.p2meshandroid.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.p2meshandroid.data.repository.WalletInfo
import com.example.p2meshandroid.presentation.wallet.WalletViewModel
import com.example.p2meshandroid.presentation.wallet.WalletUiState
import com.example.p2meshandroid.presentation.wallet.SendPaymentState
import com.example.p2meshandroid.presentation.wallet.TransactionItem
import com.example.p2meshandroid.presentation.wallet.TransactionType
import com.example.p2meshandroid.presentation.wallet.TransactionStatus
import com.example.p2meshandroid.ui.theme.*

@Composable
fun HomeScreen(
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by walletViewModel.uiState.collectAsState()
    val sendState by walletViewModel.sendState.collectAsState()
    val transactions by walletViewModel.transactions.collectAsState()
    var showSendDialog by remember { mutableStateOf(false) }
    var showFundDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when (val state = uiState) {
            is WalletUiState.Loading -> {
                LoadingScreen()
            }

            is WalletUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onCreateWallet = { walletViewModel.createNewWallet() }
                )
            }

            is WalletUiState.Success -> {
                HomeContent(
                    walletInfo = state.walletInfo,
                    transactions = transactions,
                    onSendClick = { showSendDialog = true },
                    onReceiveClick = { /* Navigate to Scan */ },
                    onFundClick = { showFundDialog = true },
                    onRefresh = { walletViewModel.refreshWallet() }
                )
            }
        }

        // Send Payment Dialog
        if (showSendDialog) {
            SendPaymentDialog(
                sendState = sendState,
                onSend = { recipient, amount -> walletViewModel.sendPayment(recipient, amount) },
                onDismiss = {
                    showSendDialog = false
                    walletViewModel.resetSendState()
                }
            )
        }

        // Fund Wallet Dialog (Faucet)
        if (showFundDialog) {
            FundWalletDialog(
                onFund = { amount -> walletViewModel.fundFromFaucet(amount) },
                onDismiss = { showFundDialog = false }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryPurple)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading wallet...",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onCreateWallet: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Welcome to P2Mesh",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onCreateWallet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple
                )
            ) {
                Text(
                    text = "Create New Wallet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    walletInfo: WalletInfo,
    transactions: List<TransactionItem>,
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onFundClick: () -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            HeaderSection()
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Balance Card
        item {
            BalanceCard(walletInfo = walletInfo)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Quick Actions
        item {
            QuickActionsSection(
                onSendClick = onSendClick,
                onReceiveClick = onReceiveClick,
                onFundClick = onFundClick,
                onRefresh = onRefresh
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Wallet Details Section
        item {
            WalletDetailsCard(walletInfo = walletInfo)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Recent Activity Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (transactions.isNotEmpty()) {
                    Text(
                        text = "${transactions.size} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Transactions or Empty state
        if (transactions.isEmpty()) {
            item {
                EmptyTransactionsCard()
            }
        } else {
            items(transactions.take(10).size) { index ->
                TransactionCard(transaction = transactions[index])
                if (index < transactions.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "P2Mesh Wallet",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Decentralized payments",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        IconButton(
            onClick = { /* Notifications */ },
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun BalanceCard(walletInfo: WalletInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BalanceCardGradientStart,
                            PrimaryPurpleDark.copy(alpha = 0.6f),
                            BalanceCardGradientEnd
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${walletInfo.balance}",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "credits",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BalanceInfoItem(label = "Available", value = "${walletInfo.availableBalance}")
                    BalanceInfoItem(label = "UTXOs", value = "${walletInfo.utxoCount}")
                    BalanceInfoItem(label = "Transactions", value = "${walletInfo.transactionCount}")
                }
            }
        }
    }
}

@Composable
private fun BalanceInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun QuickActionsSection(
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onFundClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Filled.ArrowUpward,
            label = "Send",
            onClick = onSendClick
        )
        QuickActionButton(
            icon = Icons.Filled.ArrowDownward,
            label = "Receive",
            onClick = onReceiveClick
        )
        QuickActionButton(
            icon = Icons.Filled.Add,
            label = "Fund",
            onClick = onFundClick,
            highlight = true
        )
        QuickActionButton(
            icon = Icons.Filled.Refresh,
            label = "Refresh",
            onClick = onRefresh
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    brush = if (highlight) {
                        Brush.linearGradient(
                            colors = listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.7f))
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(PrimaryPurpleDark, PrimaryPurple)
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) SuccessGreen else TextSecondary
        )
    }
}

@Composable
private fun WalletDetailsCard(walletInfo: WalletInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Wallet Details",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(label = "DID", value = walletInfo.shortDid, isMono = true)
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                label = "Public Key",
                value = walletInfo.publicKeyHex.take(20) + "...",
                isMono = true
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}

@Composable
private fun EmptyTransactionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Text(
                text = "Tap 'Fund' to get started with demo credits",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun TransactionCard(transaction: TransactionItem) {
    val isSent = transaction.type == TransactionType.SENT
    val icon = if (isSent) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    val iconColor = if (isSent) ErrorRed else SuccessGreen
    val amountPrefix = if (isSent) "-" else "+"

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
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSent) "Sent" else "Received",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.shortCounterparty,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Amount and time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${transaction.amount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = iconColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = transaction.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun SendPaymentDialog(
    sendState: SendPaymentState,
    onSend: (String, ULong) -> Unit,
    onDismiss: () -> Unit
) {
    var recipientDid by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Send Payment",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                when (sendState) {
                    is SendPaymentState.Idle -> {
                        OutlinedTextField(
                            value = recipientDid,
                            onValueChange = { recipientDid = it },
                            label = { Text("Recipient DID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = TextMuted,
                                focusedLabelColor = PrimaryPurple,
                                cursorColor = PrimaryPurple
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it.filter { c -> c.isDigit() } },
                            label = { Text("Amount") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = TextMuted,
                                focusedLabelColor = PrimaryPurple,
                                cursorColor = PrimaryPurple
                            )
                        )
                    }
                    is SendPaymentState.Sending -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryPurple)
                        }
                    }
                    is SendPaymentState.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Payment sent!", color = SuccessGreen)
                            Text(text = "Amount: ${sendState.amount} credits", color = TextSecondary)
                        }
                    }
                    is SendPaymentState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = sendState.message, color = ErrorRed, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (sendState) {
                is SendPaymentState.Idle -> {
                    Button(
                        onClick = {
                            val amountValue = amount.toULongOrNull()
                            if (recipientDid.isNotBlank() && amountValue != null && amountValue > 0u) {
                                onSend(recipientDid, amountValue)
                            }
                        },
                        enabled = recipientDid.isNotBlank() && amount.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Send")
                    }
                }
                is SendPaymentState.Success, is SendPaymentState.Error -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Done")
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (sendState is SendPaymentState.Idle) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}

@Composable
private fun FundWalletDialog(
    onFund: (ULong) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Fund Wallet",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Get demo credits from the faucet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuccessGreen,
                        unfocusedBorderColor = TextMuted,
                        focusedLabelColor = SuccessGreen,
                        cursorColor = SuccessGreen
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("50", "100", "500", "1000").forEach { preset ->
                        FilterChip(
                            selected = amount == preset,
                            onClick = { amount = preset },
                            label = { Text(preset) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SuccessGreen.copy(alpha = 0.2f),
                                selectedLabelColor = SuccessGreen
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toULongOrNull()
                    if (amountValue != null && amountValue > 0u) {
                        onFund(amountValue)
                        onDismiss()
                    }
                },
                enabled = amount.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Text("Fund")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
