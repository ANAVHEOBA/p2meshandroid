package com.example.p2meshandroid.presentation.mesh

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.p2meshandroid.data.bluetooth.BluetoothPeer
import com.example.p2meshandroid.data.wifidirect.WiFiDirectPeer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.p2meshandroid.di.MeshViewModelFactory
import com.example.p2meshandroid.ui.theme.*
import com.example.p2meshandroid.util.BluetoothPermissionHelper

@Composable
fun MeshScreen(
    meshViewModel: MeshViewModel = viewModel(factory = MeshViewModelFactory()),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by meshViewModel.uiState.collectAsState()
    val connectionState by meshViewModel.connectionState.collectAsState()
    val bluetoothState by meshViewModel.bluetoothState.collectAsState()
    val discoveredBluetoothPeers by meshViewModel.discoveredBluetoothPeers.collectAsState()
    val connectedBluetoothPeers by meshViewModel.connectedBluetoothPeers.collectAsState()
    val wifiDirectState by meshViewModel.wifiDirectState.collectAsState()
    val discoveredWiFiDirectPeers by meshViewModel.discoveredWiFiDirectPeers.collectAsState()
    var showConnectDialog by remember { mutableStateOf(false) }
    var peerAddress by remember { mutableStateOf("") }
    var hasBluetoothPermissions by remember { mutableStateOf(BluetoothPermissionHelper.hasAllPermissions(context)) }

    // Permission launcher for Bluetooth
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBluetoothPermissions = permissions.values.all { it }
        if (hasBluetoothPermissions) {
            meshViewModel.startBluetoothMesh()
        }
    }

    // Function to request permissions and start Bluetooth
    val requestBluetoothPermissionsAndStart: () -> Unit = {
        if (BluetoothPermissionHelper.hasAllPermissions(context)) {
            meshViewModel.startBluetoothMesh()
        } else {
            bluetoothPermissionLauncher.launch(
                BluetoothPermissionHelper.getRequiredPermissions().toTypedArray()
            )
        }
    }

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
            BluetoothTransportCard(
                bluetoothState = bluetoothState,
                discoveredPeersCount = discoveredBluetoothPeers.size,
                connectedPeersCount = connectedBluetoothPeers.size,
                hasPermissions = hasBluetoothPermissions,
                onStart = requestBluetoothPermissionsAndStart,
                onStop = { meshViewModel.stopBluetoothMesh() }
            )
        }

        // Discovered Bluetooth Peers
        if (discoveredBluetoothPeers.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discovered Bluetooth Peers",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(discoveredBluetoothPeers) { peer ->
                BluetoothPeerCard(
                    peer = peer,
                    onConnect = { meshViewModel.connectToBluetoothPeer(peer.address) },
                    onSync = { meshViewModel.syncWithBluetoothPeer(peer.address) },
                    onDisconnect = { meshViewModel.disconnectFromBluetoothPeer(peer.address) }
                )
            }
        }

        // WiFi Direct Transport Card
        item {
            WiFiDirectTransportCard(
                wifiDirectState = wifiDirectState,
                discoveredPeersCount = discoveredWiFiDirectPeers.size,
                onStartDiscovery = { meshViewModel.startWiFiDirectDiscovery() },
                onStopDiscovery = { meshViewModel.stopWiFiDirectDiscovery() },
                onCreateGroup = { meshViewModel.createWiFiDirectGroup() },
                onDisconnect = { meshViewModel.disconnectWiFiDirect() },
                onSync = { meshViewModel.syncViaWiFiDirect() }
            )
        }

        // Discovered WiFi Direct Peers
        if (discoveredWiFiDirectPeers.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WiFi Direct Peers",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(discoveredWiFiDirectPeers) { peer ->
                WiFiDirectPeerCard(
                    peer = peer,
                    onConnect = { meshViewModel.connectToWiFiDirectPeer(peer.deviceAddress) }
                )
            }
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

@Composable
private fun BluetoothTransportCard(
    bluetoothState: BluetoothUiState,
    discoveredPeersCount: Int,
    connectedPeersCount: Int,
    hasPermissions: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isActive = bluetoothState is BluetoothUiState.Active ||
            bluetoothState is BluetoothUiState.Scanning ||
            bluetoothState is BluetoothUiState.Advertising

    val statusColor = when {
        !hasPermissions -> WarningYellow
        bluetoothState is BluetoothUiState.Active -> SuccessGreen
        bluetoothState is BluetoothUiState.Scanning || bluetoothState is BluetoothUiState.Advertising -> WarningYellow
        bluetoothState is BluetoothUiState.Error -> ErrorRed
        bluetoothState is BluetoothUiState.Disabled -> ErrorRed
        else -> PrimaryPurple
    }

    val statusText = when {
        !hasPermissions -> "Tap to grant permissions"
        else -> when (bluetoothState) {
            is BluetoothUiState.Unavailable -> "Not available"
            is BluetoothUiState.Disabled -> "Bluetooth disabled"
            is BluetoothUiState.Enabling -> "Enabling..."
            is BluetoothUiState.Ready -> "Ready to connect"
            is BluetoothUiState.Starting -> "Starting..."
            is BluetoothUiState.Scanning -> "Scanning... ($discoveredPeersCount found)"
            is BluetoothUiState.Advertising -> "Advertising..."
            is BluetoothUiState.Active -> "$connectedPeersCount connected, $discoveredPeersCount nearby"
            is BluetoothUiState.Error -> bluetoothState.message
        }
    }

    val isEnabled = hasPermissions &&
            bluetoothState !is BluetoothUiState.Unavailable &&
            bluetoothState !is BluetoothUiState.Disabled

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
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bluetooth LE",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bluetoothState is BluetoothUiState.Error) ErrorRed else TextSecondary
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = { checked ->
                    if (checked) onStart() else onStop()
                },
                // Allow enabling when permissions not granted (will trigger permission request)
                enabled = (bluetoothState !is BluetoothUiState.Unavailable &&
                        bluetoothState !is BluetoothUiState.Starting) || !hasPermissions,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryPurple,
                    checkedTrackColor = PrimaryPurple.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun BluetoothPeerCard(
    peer: BluetoothPeer,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit
) {
    val signalStrength = when {
        peer.rssi >= -50 -> "Excellent"
        peer.rssi >= -70 -> "Good"
        peer.rssi >= -80 -> "Fair"
        else -> "Weak"
    }

    val signalColor = when {
        peer.rssi >= -50 -> SuccessGreen
        peer.rssi >= -70 -> PrimaryPurple
        peer.rssi >= -80 -> WarningYellow
        else -> ErrorRed
    }

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
                    .background(if (peer.isConnected) SuccessGreen.copy(alpha = 0.2f) else PrimaryPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = if (peer.isConnected) SuccessGreen else PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${peer.address} • $signalStrength (${peer.rssi} dBm)",
                    style = MaterialTheme.typography.bodySmall,
                    color = signalColor
                )
            }
            if (peer.isConnected) {
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
            } else {
                IconButton(onClick = onConnect) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = "Connect",
                        tint = PrimaryPurple
                    )
                }
            }
        }
    }
}

@Composable
private fun WiFiDirectTransportCard(
    wifiDirectState: WiFiDirectUiState,
    discoveredPeersCount: Int,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onCreateGroup: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit
) {
    val isActive = wifiDirectState is WiFiDirectUiState.Discovering ||
            wifiDirectState is WiFiDirectUiState.Connected ||
            wifiDirectState is WiFiDirectUiState.GroupFormed

    val isGroupFormed = wifiDirectState is WiFiDirectUiState.GroupFormed

    val statusColor = when (wifiDirectState) {
        is WiFiDirectUiState.GroupFormed -> SuccessGreen
        is WiFiDirectUiState.Connected -> SuccessGreen
        is WiFiDirectUiState.Discovering -> WarningYellow
        is WiFiDirectUiState.Connecting -> WarningYellow
        is WiFiDirectUiState.Error -> ErrorRed
        is WiFiDirectUiState.Disabled, is WiFiDirectUiState.Unavailable -> ErrorRed
        else -> PrimaryPurple
    }

    val statusText = when (wifiDirectState) {
        is WiFiDirectUiState.Unavailable -> "Not available"
        is WiFiDirectUiState.Disabled -> "WiFi disabled"
        is WiFiDirectUiState.Ready -> "Ready to discover"
        is WiFiDirectUiState.Starting -> "Starting..."
        is WiFiDirectUiState.Discovering -> "Discovering... ($discoveredPeersCount found)"
        is WiFiDirectUiState.Connecting -> "Connecting..."
        is WiFiDirectUiState.Connected -> "Connected"
        is WiFiDirectUiState.GroupFormed -> {
            if (wifiDirectState.isOwner) "Group Owner" else "Connected to group"
        }
        is WiFiDirectUiState.Error -> wifiDirectState.message
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiTethering,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WiFi Direct",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (wifiDirectState is WiFiDirectUiState.Error) ErrorRed else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Discover button
                if (!isActive) {
                    Button(
                        onClick = onStartDiscovery,
                        enabled = wifiDirectState !is WiFiDirectUiState.Unavailable &&
                                wifiDirectState !is WiFiDirectUiState.Disabled,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Discover")
                    }

                    Button(
                        onClick = onCreateGroup,
                        enabled = wifiDirectState !is WiFiDirectUiState.Unavailable &&
                                wifiDirectState !is WiFiDirectUiState.Disabled,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GroupAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create")
                    }
                } else {
                    if (isGroupFormed) {
                        Button(
                            onClick = onSync,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (wifiDirectState is WiFiDirectUiState.Discovering) {
                                onStopDiscovery()
                            } else {
                                onDisconnect()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = ErrorRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (wifiDirectState is WiFiDirectUiState.Discovering) "Stop" else "Disconnect",
                            color = ErrorRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WiFiDirectPeerCard(
    peer: WiFiDirectPeer,
    onConnect: () -> Unit
) {
    val statusColor = when (peer.status) {
        android.net.wifi.p2p.WifiP2pDevice.AVAILABLE -> SuccessGreen
        android.net.wifi.p2p.WifiP2pDevice.CONNECTED -> PrimaryPurple
        android.net.wifi.p2p.WifiP2pDevice.INVITED -> WarningYellow
        else -> TextMuted
    }

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
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiTethering,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.deviceName.ifEmpty { "Unknown Device" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${peer.deviceAddress} • ${peer.statusText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
            if (peer.status == android.net.wifi.p2p.WifiP2pDevice.AVAILABLE) {
                IconButton(onClick = onConnect) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = "Connect",
                        tint = PrimaryPurple
                    )
                }
            }
        }
    }
}
