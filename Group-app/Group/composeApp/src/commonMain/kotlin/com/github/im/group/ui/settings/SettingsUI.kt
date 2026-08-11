package com.github.im.group.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.config.AppEnvironment
import com.github.im.group.repository.NetworkSettingsDraft
import com.github.im.group.update.AppUpdatePhase
import com.github.im.group.update.AppUpdateUiState
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsUI(
    navHostController: NavHostController
) {
    val chatViewModel: ChatViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val chatListState by chatViewModel.uiState.collectAsState()
    val uiState by settingsViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8FBFF), Color(0xFFFFFFFF))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            MessageSummaryCard(
                unreadCount = chatListState.totalUnreadCount,
                readAllInProgress = chatListState.readAllInProgress,
                onMarkAllRead = chatViewModel::markAllConversationsRead
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Notifications") {
                SettingToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Enable message notifications",
                    description = "Allow chat alerts and system notifications on this device.",
                    checked = uiState.notification.enableNotifications,
                    onCheckedChange = settingsViewModel::updateNotificationsEnabled
                )
                HorizontalDivider()
                SettingToggleItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Play alert sound",
                    description = "Play a sound when a new message arrives.",
                    checked = uiState.notification.enableSound,
                    onCheckedChange = settingsViewModel::updateNotificationSound
                )
                HorizontalDivider()
                SettingToggleItem(
                    icon = Icons.Default.DoneAll,
                    title = "Show message preview",
                    description = "Display preview text inside notifications.",
                    checked = uiState.notification.enablePreview,
                    onCheckedChange = settingsViewModel::updateNotificationPreview
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Network And Proxy") {
                NetworkEnvironmentSelector(
                    currentEnvironment = uiState.networkDraft.environment,
                    onSelect = settingsViewModel::setNetworkEnvironment
                )
                HorizontalDivider()
                NetworkSettingsEditor(
                    draft = uiState.networkDraft,
                    currentBaseUrl = uiState.network.currentBaseUrl,
                    isSaving = uiState.isSavingNetwork,
                    isDirty = uiState.isNetworkDirty,
                    onApiHostChange = settingsViewModel::setApiHost,
                    onApiPortChange = settingsViewModel::setApiPort,
                    onTcpHostChange = settingsViewModel::setTcpHost,
                    onTcpPortChange = settingsViewModel::setTcpPort,
                    onUseTlsChange = settingsViewModel::setUseTls,
                    onReset = settingsViewModel::resetNetworkDraft,
                    onSave = settingsViewModel::saveNetworkSettings
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Remote Settings Roadmap") {
                SettingItem(
                    icon = Icons.Default.Security,
                    title = "Privacy And Security",
                    description = "Account synced settings such as online visibility and friend request policy will move here.",
                    onClick = { }
                )
                HorizontalDivider()
                SettingItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Video And Meeting Preferences",
                    description = "Video quality, auto join audio, and meeting defaults will become remote user settings.",
                    onClick = { }
                )
                HorizontalDivider()
                SettingItem(
                    icon = Icons.Default.Storage,
                    title = "Storage And Device",
                    description = "Download directory, cache policy, and device defaults remain local settings.",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            uiState.appUpdate?.let { updateState ->
                SettingsSection(title = "App Update") {
                    AppUpdateSettings(
                        state = updateState,
                        onCheck = settingsViewModel::checkForAppUpdate,
                        onDownloadOrInstall = settingsViewModel::downloadOrInstallUpdate,
                        onAutoDownloadChanged = settingsViewModel::setAutoDownloadOnWifi
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            SettingsSection(title = "About") {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "Settings architecture",
                    description = "This screen is now the unified entry point for local settings and future remote settings.",
                    onClick = { }
                )
                HorizontalDivider()
                SettingItem(
                    icon = Icons.Default.Update,
                    title = "Current version",
                    description = uiState.appUpdate?.currentVersion?.let { "v$it" } ?: "Unavailable on this platform",
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun AppUpdateSettings(
    state: AppUpdateUiState,
    onCheck: () -> Unit,
    onDownloadOrInstall: () -> Unit,
    onAutoDownloadChanged: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        val update = state.update
        Text(
            text = when (state.phase) {
                AppUpdatePhase.CHECKING -> "Checking for updates…"
                AppUpdatePhase.AVAILABLE -> update?.let { "Version ${it.versionName} is available" } ?: "No update available"
                AppUpdatePhase.DOWNLOADING -> "Downloading update in the background"
                AppUpdatePhase.READY_TO_INSTALL -> "Update is ready to install"
                AppUpdatePhase.FAILED -> "Update failed"
                else -> "Current version ${state.currentVersion}"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        update?.changelog?.takeIf { it.isNotBlank() }?.let { changelog ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(changelog, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.message?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (state.phase == AppUpdatePhase.DOWNLOADING && state.totalBytes > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${state.downloadedBytes / 1024 / 1024} MB / ${state.totalBytes / 1024 / 1024} MB",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-download on Wi‑Fi", style = MaterialTheme.typography.bodyLarge)
                Text("Downloads are verified before the installer is opened.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.autoDownloadOnWifi, onCheckedChange = onAutoDownloadChanged)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCheck, enabled = state.phase != AppUpdatePhase.CHECKING) { Text("Check now") }
            if (update != null) {
                Button(onClick = onDownloadOrInstall, enabled = state.phase != AppUpdatePhase.CHECKING) {
                    Text(if (state.phase == AppUpdatePhase.READY_TO_INSTALL) "Install update" else "Download")
                }
            }
        }
    }
}

@Composable
private fun MessageSummaryCard(
    unreadCount: Int,
    readAllInProgress: Boolean,
    onMarkAllRead: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1D4ED8))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Message Center",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (unreadCount > 0) {
                        "You still have $unreadCount unread messages. You can clear them in one tap here."
                    } else {
                        "No unread messages right now. Realtime alerts will continue in the background."
                    },
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onMarkAllRead,
                    enabled = unreadCount > 0 && !readAllInProgress,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (readAllInProgress) "Processing..." else "Mark all as read")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    description: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
                .background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun NetworkEnvironmentSelector(
    currentEnvironment: AppEnvironment,
    onSelect: (AppEnvironment) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Connection profile",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Use a shared environment or switch to a custom local proxy for development.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                AppEnvironment.DEV,
                AppEnvironment.TEST,
                AppEnvironment.PROD,
                AppEnvironment.CUSTOM
            ).forEach { environment ->
                val selected = currentEnvironment == environment
                OutlinedButton(
                    onClick = { onSelect(environment) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Text(environment.name)
                }
            }
        }
    }
}

@Composable
private fun NetworkSettingsEditor(
    draft: NetworkSettingsDraft,
    currentBaseUrl: String,
    isSaving: Boolean,
    isDirty: Boolean,
    onApiHostChange: (String) -> Unit,
    onApiPortChange: (String) -> Unit,
    onTcpHostChange: (String) -> Unit,
    onTcpPortChange: (String) -> Unit,
    onUseTlsChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Effective base URL: $currentBaseUrl",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (draft.isCustom) {
                "Custom mode stores the local server override on this device only."
            } else {
                "Standard environments use shared preset hosts. Switch to CUSTOM to edit local network values."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (draft.isCustom) {
            OutlinedTextField(
                value = draft.apiHost,
                onValueChange = onApiHostChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API host") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = draft.apiPort,
                onValueChange = onApiPortChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = draft.tcpHost,
                onValueChange = onTcpHostChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("TCP host") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = draft.tcpPort,
                onValueChange = onTcpPortChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("TCP port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = draft.useTls,
                    onCheckedChange = onUseTlsChange
                )
                Text("Use TLS")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                enabled = isDirty && !isSaving
            ) {
                Text("Reset")
            }
            Button(
                onClick = onSave,
                enabled = isDirty && !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        }
    }
}
