package com.example.p2meshandroid.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.p2meshandroid.presentation.wallet.WalletViewModel
import com.example.p2meshandroid.presentation.wallet.WalletUiState
import com.example.p2meshandroid.ui.theme.*

@Composable
fun SettingsScreen(
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by walletViewModel.uiState.collectAsState()
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Manage your wallet",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Wallet Section
        SettingsSection(title = "Wallet") {
            SettingsItem(
                icon = Icons.Outlined.Backup,
                title = "Backup Wallet",
                description = "Export your secret key",
                onClick = { showBackupDialog = true }
            )
            SettingsItem(
                icon = Icons.Outlined.Restore,
                title = "Restore Wallet",
                description = "Import from secret key",
                onClick = { showRestoreDialog = true }
            )
            SettingsItem(
                icon = Icons.Outlined.Add,
                title = "Create New Wallet",
                description = "Generate a new identity",
                onClick = { walletViewModel.createNewWallet() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Section
        SettingsSection(title = "Security") {
            SettingsItem(
                icon = Icons.Outlined.Lock,
                title = "App Lock",
                description = "Coming soon",
                enabled = false
            )
            SettingsItem(
                icon = Icons.Outlined.Fingerprint,
                title = "Biometric Authentication",
                description = "Coming soon",
                enabled = false
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Network Section
        SettingsSection(title = "Network") {
            SettingsItem(
                icon = Icons.Outlined.Cloud,
                title = "Settlement Server",
                description = "Not connected",
                enabled = false
            )
            SettingsItem(
                icon = Icons.Outlined.Speed,
                title = "Sync Frequency",
                description = "Coming soon",
                enabled = false
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        SettingsSection(title = "About") {
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "Version",
                description = "1.0.0 (Demo)",
                showArrow = false
            )
            SettingsItem(
                icon = Icons.Outlined.Code,
                title = "Open Source",
                description = "View on GitHub",
                onClick = { /* TODO */ }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Danger Zone
        Text(
            text = "Danger Zone",
            style = MaterialTheme.typography.titleSmall,
            color = ErrorRed,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    tint = ErrorRed
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reset Wallet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ErrorRed
                    )
                    Text(
                        text = "Delete all data and start fresh",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // Backup Dialog
    if (showBackupDialog) {
        BackupDialog(
            walletViewModel = walletViewModel,
            onDismiss = { showBackupDialog = false }
        )
    }

    // Restore Dialog
    if (showRestoreDialog) {
        RestoreDialog(
            onRestore = { secretKey ->
                // TODO: Implement restore
                showRestoreDialog = false
            },
            onDismiss = { showRestoreDialog = false }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Reset Wallet?", color = ErrorRed, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will delete your wallet and all data. Make sure you have backed up your secret key!",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        walletViewModel.createNewWallet()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) PrimaryPurple else TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) TextPrimary else TextMuted
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        if (showArrow && enabled) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BackupDialog(
    walletViewModel: WalletViewModel,
    onDismiss: () -> Unit
) {
    var secretKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        walletViewModel.getBackup { result ->
            result.onSuccess { backup ->
                secretKey = backup.secretKeyHex
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Backup Wallet", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Save this secret key securely. Anyone with this key can access your wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningYellow
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (secretKey != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkBackground)
                    ) {
                        Text(
                            text = secretKey!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        color = PrimaryPurple,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { /* TODO: Copy to clipboard */ },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun RestoreDialog(
    onRestore: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var secretKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Restore Wallet", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Enter your secret key to restore your wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    label = { Text("Secret Key (hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = TextMuted,
                        focusedLabelColor = PrimaryPurple,
                        cursorColor = PrimaryPurple
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onRestore(secretKey) },
                enabled = secretKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
