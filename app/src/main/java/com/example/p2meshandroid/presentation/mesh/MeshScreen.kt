package com.example.p2meshandroid.presentation.mesh

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.p2meshandroid.di.MeshViewModelFactory
import com.example.p2meshandroid.ui.theme.*

@Composable
fun MeshScreen(
    meshViewModel: MeshViewModel = viewModel(factory = MeshViewModelFactory()),
    modifier: Modifier = Modifier
) {
    val uiState by meshViewModel.uiState.collectAsState()
    val connectionState by meshViewModel.connectionState.collectAsState()
    var showConnectDialog by remember { mutableStateOf(false) }
    var peerAddress by remember { mutableStateOf("") }

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
                text = "Mesh Network",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "P2P synchronization status",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Connection Status Card
        item {
            ConnectionStatusCard(
                uiState = uiState,
                connectionState = connectionState,
                onStartTransport = { meshViewModel.startTransport() },
                onStopTransport = { meshViewModel.stopTransport() }
            )
        }

        // Connect to Peer Section (only when transport is running)
        if (uiState is MeshUiState.Connected) {
            item {
                ConnectToPeerCard(
                    onConnect = { showConnectDialog = true }
                )
            }
        }

        // Connected Peers List
        val connectedState = uiState as? MeshUiState.Connected
        if (connectedState != null && connectedState.connectedPeers.isNotEmpty()) {
            item {
                Text(
                    text = "Connected Peers",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(connectedState.connectedPeers) { peer ->
                PeerCard(
                    address = peer.address,
                    transportType = peer.transportType,
                    onSync = { meshViewModel.syncWithPeer(peer.address) },
                    onDisconnect = { meshViewModel.disconnectFromPeer(peer.address) }
                )
            }
        }

        // Transport Options
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Transport Options",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            TransportOptionCard(
                icon = Icons.Filled.Wifi,
                title = "TCP/IP",
                description = if (uiState is MeshUiState.Connected)
                    "Running on ${(uiState as MeshUiState.Connected).bindAddress}"
                else "Connect over local network",
                enabled = uiState is MeshUiState.Connected,
                onToggle = { enabled ->
                    if (enabled) meshViewModel.startTransport()
                    else meshViewModel.stopTransport()
                }
            )
        }

        item {
            TransportOptionCard(
                icon = Icons.Filled.Bluetooth,
                title = "Bluetooth",
                description = "Coming soon",
                enabled = false,
                onToggle = { },
                isComingSoon = true
            )
        }

        item {
            TransportOptionCard(
                icon = Icons.Filled.Radio,
                title = "LoRa",
                description = "Coming soon",
                enabled = false,
                onToggle = { },
                isComingSoon = true
            )
        }
    }

    // Connect Dialog
    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = { Text("Connect to Peer", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = peerAddress,
                    onValueChange = { peerAddress = it },
                    label = { Text("Peer Address", color = TextMuted) },
                    placeholder = { Text("192.168.1.100:8080", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryPurple
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (peerAddress.isNotBlank()) {
                            meshViewModel.connectToPeer(peerAddress)
                            showConnectDialog = false
                            peerAddress = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun ConnectionStatusCard(
    uiState: MeshUiState,
    connectionState: ConnectionState,
    onStartTransport: () -> Unit,
    onStopTransport: () -> Unit
) {
    val isConnected = uiState is MeshUiState.Connected
    val statusColor = if (isConnected) SuccessGreen else WarningYellow
    val statusIcon = if (isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff

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
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConnected) "Transport Running" else "Not Connected",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (connectionState) {
                            is ConnectionState.Starting -> "Starting transport..."
                            is ConnectionState.Stopping -> "Stopping..."
                            is ConnectionState.Connecting -> "Connecting to ${connectionState.address}..."
                            is ConnectionState.Syncing -> "Syncing with ${connectionState.address}..."
                            is ConnectionState.Error -> connectionState.message
                            else -> if (isConnected) {
                                val state = uiState as MeshUiState.Connected
                                "${state.peerCount} peers connected"
                            } else "Tap to start"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (connectionState is ConnectionState.Error) ErrorRed else TextSecondary
                    )
                }

                // Start/Stop Button
                IconButton(
                    onClick = if (isConnected) onStopTransport else onStartTransport,
                    enabled = connectionState is ConnectionState.Idle ||
                              connectionState is ConnectionState.Error ||
                              connectionState is ConnectionState.SyncComplete
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isConnected) "Stop" else "Start",
                        tint = if (isConnected) ErrorRed else SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val stats = when (uiState) {
                    is MeshUiState.Connected -> Triple(
                        uiState.peerCount.toString(),
                        uiState.iouCount.toString(),
                        if (uiState.totalSyncs > 0UL) uiState.totalSyncs.toString() else "0"
                    )
                    is MeshUiState.Disconnected -> Triple(
                        "0",
                        uiState.iouCount.toString(),
                        uiState.totalSyncs.toString()
                    )
                }
                StatItem(label = "Peers", value = stats.first)
                StatItem(label = "IOUs", value = stats.second)
                StatItem(label = "Syncs", value = stats.third)
            }
        }
    }
}

@Composable
private fun ConnectToPeerCard(onConnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.1f)),
        onClick = onConnect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = null,
                tint = PrimaryPurple
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connect to Peer",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryPurple,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Enter peer IP address to connect",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = PrimaryPurple
            )
        }
    }
}

@Composable
private fun PeerCard(
    address: String,
    transportType: String,
    onSync: () -> Unit,
    onDisconnect: () -> Unit
) {
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
                    .background(SuccessGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Connected via $transportType",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen
                )
            }
            IconButton(onClick = onSync) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = "Sync",
                    tint = PrimaryPurple
                )
            }
            IconButton(onClick = onDisconnect) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Disconnect",
                    tint = ErrorRed
                )
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
private fun TransportOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isComingSoon: Boolean = false
) {
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isComingSoon) TextMuted else if (enabled) SuccessGreen else PrimaryPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isComingSoon) TextMuted else TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            if (isComingSoon) {
                Text(
                    text = "Soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            } else {
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryPurple,
                        checkedTrackColor = PrimaryPurple.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}
