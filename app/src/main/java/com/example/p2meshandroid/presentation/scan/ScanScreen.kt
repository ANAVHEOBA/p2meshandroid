package com.example.p2meshandroid.presentation.scan

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.p2meshandroid.presentation.wallet.WalletViewModel
import com.example.p2meshandroid.presentation.wallet.WalletUiState
import com.example.p2meshandroid.presentation.wallet.ReceivePaymentState
import com.example.p2meshandroid.presentation.wallet.SendPaymentState
import com.example.p2meshandroid.ui.theme.*
import com.example.p2meshandroid.util.QrCodeGenerator
import android.content.Intent
import android.graphics.Color as AndroidColor

@Composable
fun ScanScreen(
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by walletViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        // Header
        Text(
            text = "Send & Receive",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Transfer credits via IOUs",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = PrimaryPurple
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Send") },
                selectedContentColor = PrimaryPurple,
                unselectedContentColor = TextSecondary
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Receive") },
                selectedContentColor = PrimaryPurple,
                unselectedContentColor = TextSecondary
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("My DID") },
                selectedContentColor = PrimaryPurple,
                unselectedContentColor = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (selectedTab) {
            0 -> SendTab(walletViewModel, uiState)
            1 -> ReceiveTab(walletViewModel)
            2 -> MyDidTab(uiState)
        }
    }
}

@Composable
private fun SendTab(walletViewModel: WalletViewModel, uiState: WalletUiState) {
    val sendState by walletViewModel.sendState.collectAsState()
    var recipientDid by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Reset fields on success after delay
    LaunchedEffect(sendState) {
        if (sendState is SendPaymentState.Success) {
            kotlinx.coroutines.delay(3000)
            walletViewModel.resetSendState()
            recipientDid = ""
            amountText = ""
        }
    }

    // QR Scanner fullscreen overlay
    if (showScanner) {
        QrScannerOverlay(
            onQrCodeScanned = { scannedValue ->
                recipientDid = scannedValue
                showScanner = false
            },
            onClose = { showScanner = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Balance Card
        when (uiState) {
            is WalletUiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Available Balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${uiState.walletInfo.availableBalance} credits",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            else -> {}
        }

        // Status Card
        when (sendState) {
            is SendPaymentState.Sending -> {
                StatusCard("Creating payment...", PrimaryPurple, showProgress = true)
                Spacer(modifier = Modifier.height(16.dp))
            }
            is SendPaymentState.Success -> {
                val state = sendState as SendPaymentState.Success
                SuccessCard(paymentId = state.paymentId, amount = state.amount)
                Spacer(modifier = Modifier.height(16.dp))
            }
            is SendPaymentState.Error -> {
                StatusCard((sendState as SendPaymentState.Error).message, ErrorRed, showProgress = false)
                Spacer(modifier = Modifier.height(16.dp))
            }
            else -> {}
        }

        // Recipient DID Input
        Text(
            text = "Recipient DID",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = recipientDid,
            onValueChange = { recipientDid = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("did:key:...", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryPurple
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = TextMuted
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Paste DID button
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        recipientDid = clip.getItemAt(0).text?.toString() ?: ""
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paste")
            }

            // Scan QR button
            OutlinedButton(
                onClick = { showScanner = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple)
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan QR")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Amount Input
        Text(
            text = "Amount",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { newValue ->
                // Only allow digits
                if (newValue.all { it.isDigit() }) {
                    amountText = newValue
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter amount", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryPurple
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Paid,
                    contentDescription = null,
                    tint = TextMuted
                )
            },
            suffix = { Text("credits", color = TextMuted) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick amount buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(10, 50, 100, 500).forEach { amount ->
                FilterChip(
                    onClick = { amountText = amount.toString() },
                    label = { Text("$amount") },
                    selected = amountText == amount.toString(),
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple,
                        selectedLabelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Send Button
        val amount = amountText.toULongOrNull() ?: 0UL
        val canSend = recipientDid.isNotBlank() && amount > 0UL && sendState !is SendPaymentState.Sending

        Button(
            onClick = {
                walletViewModel.sendPayment(recipientDid.trim(), amount)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            enabled = canSend
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Send Payment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "How it works",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Enter the recipient's DID\n" +
                    "2. Enter the amount to send\n" +
                    "3. Copy the generated IOU and share it\n" +
                    "4. Recipient processes the IOU to receive funds",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    message: String,
    color: androidx.compose.ui.graphics.Color,
    showProgress: Boolean
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
                    modifier = Modifier.size(24.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = null,
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, color = color)
        }
    }
}

@Composable
private fun SuccessCard(paymentId: String, amount: ULong) {
    val context = LocalContext.current
    var showCopied by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Payment Created!",
                    style = MaterialTheme.typography.titleMedium,
                    color = SuccessGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Amount: $amount credits",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Payment ID:",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = paymentId,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (showCopied) {
                Text(
                    "Copied to clipboard!",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen
                )
            }

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Payment ID", paymentId)
                    clipboard.setPrimaryClip(clip)
                    showCopied = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Payment ID")
            }
        }
    }
}

@Composable
private fun MyDidTab(uiState: WalletUiState) {
    val context = LocalContext.current
    var showCopiedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(showCopiedMessage) {
        if (showCopiedMessage) {
            kotlinx.coroutines.delay(2000)
            showCopiedMessage = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState) {
            is WalletUiState.Success -> {
                // Generate QR Code for DID
                val qrCodeBitmap = remember(uiState.walletInfo.did) {
                    QrCodeGenerator.generateQrCode(
                        content = uiState.walletInfo.did,
                        size = 512,
                        foregroundColor = AndroidColor.BLACK,
                        backgroundColor = AndroidColor.WHITE
                    )?.asImageBitmap()
                }

                // QR Code Display
                Card(
                    modifier = Modifier.size(260.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = TextPrimary)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrCodeBitmap != null) {
                            Image(
                                bitmap = qrCodeBitmap,
                                contentDescription = "QR Code for your DID",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Fallback if QR generation fails
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode,
                                    contentDescription = null,
                                    tint = DarkBackground,
                                    modifier = Modifier.size(100.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "QR Code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkBackground
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Your DID",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.walletInfo.did,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Copied Message
                if (showCopiedMessage) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DID copied to clipboard!", color = SuccessGreen, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Copy Button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = android.content.ClipData.newPlainText("DID", uiState.walletInfo.did)
                        clipboard.setPrimaryClip(clip)
                        showCopiedMessage = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryPurple
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy DID")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Share Button
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, uiState.walletInfo.did)
                            putExtra(Intent.EXTRA_SUBJECT, "My P2Mesh DID")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share DID"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
            else -> {
                // No wallet - show placeholder
                Card(
                    modifier = Modifier.size(250.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Create a wallet first",
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiveTab(walletViewModel: WalletViewModel) {
    val receiveState by walletViewModel.receiveState.collectAsState()
    var paymentData by remember { mutableStateOf("") }
    var senderPubKey by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Card
        when (receiveState) {
            is ReceivePaymentState.Processing -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryPurple,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Processing payment...", color = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            is ReceivePaymentState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Payment received successfully!", color = SuccessGreen)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    walletViewModel.resetReceiveState()
                    paymentData = ""
                    senderPubKey = ""
                }
            }
            is ReceivePaymentState.Error -> {
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
                        Text(
                            (receiveState as ReceivePaymentState.Error).message,
                            color = ErrorRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            else -> {}
        }

        // Payment Data Input
        Text(
            text = "Paste Payment Data",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = paymentData,
            onValueChange = { paymentData = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Hex-encoded payment IOU...", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryPurple
            ),
            shape = RoundedCornerShape(12.dp),
            minLines = 3,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Paste from clipboard button
        OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    paymentData = clip.getItemAt(0).text?.toString() ?: ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(
                imageVector = Icons.Filled.ContentPaste,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Paste from Clipboard")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Advanced Options Toggle
        TextButton(
            onClick = { showAdvanced = !showAdvanced }
        ) {
            Icon(
                imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Advanced Options",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (showAdvanced) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sender Public Key (optional)",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = senderPubKey,
                onValueChange = { senderPubKey = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Hex-encoded public key...", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = PrimaryPurple
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Process Payment Button
        Button(
            onClick = {
                val cleanData = paymentData.trim().replace("\\s".toRegex(), "")
                if (senderPubKey.isNotBlank()) {
                    val cleanKey = senderPubKey.trim().replace("\\s".toRegex(), "")
                    walletViewModel.receivePaymentWithKey(cleanData, cleanKey)
                } else {
                    walletViewModel.receivePayment(cleanData)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            enabled = paymentData.isNotBlank() && receiveState !is ReceivePaymentState.Processing
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Process Payment")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "How to receive payments",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Get the payment IOU data from the sender\n" +
                    "2. Paste the hex-encoded data above\n" +
                    "3. Tap 'Process Payment' to add to your balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

/**
 * QR Scanner overlay composable with permission handling
 */
@Composable
private fun QrScannerOverlay(
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Request permission on first launch if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (hasCameraPermission) {
            // Camera preview with scanner
            CameraPreviewScanner(
                onQrCodeScanned = onQrCodeScanned
            )

            // Scanning frame overlay
            Box(modifier = Modifier.fillMaxSize()) {
                // Semi-transparent background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground.copy(alpha = 0.6f))
                )

                // Clear scanning area
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(androidx.compose.ui.graphics.Color.Transparent)
                )
            }

            // Scanning frame border
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .border(3.dp, PrimaryPurple, RoundedCornerShape(16.dp))
            )

            // Instructions
            Text(
                text = "Point camera at QR code",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .background(DarkSurface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        } else {
            // Permission not granted UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please grant camera permission to scan QR codes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(DarkSurface.copy(alpha = 0.8f), RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close scanner",
                tint = TextPrimary
            )
        }

        // Title
        Text(
            text = "Scan QR Code",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )
    }
}

/**
 * Camera preview with barcode scanning
 */
@Composable
private fun CameraPreviewScanner(
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastScannedValue by remember { mutableStateOf<String?>(null) }
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            androidx.camera.view.PreviewView(ctx).apply {
                scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER

                val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview
                    val preview = androidx.camera.core.Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = surfaceProvider
                        }

                    // Image analysis for barcode
                    val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(1280, 720))
                        .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val barcodeScanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        @androidx.camera.core.ExperimentalGetImage
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        if (barcode.format == com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE) {
                                            barcode.rawValue?.let { value ->
                                                if (value != lastScannedValue) {
                                                    lastScannedValue = value
                                                    onQrCodeScanned(value)
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    // Back camera
                    val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
