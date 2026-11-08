package com.example.ui.screens.settings

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.BiometricHelper
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.VaultAccent
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultError
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceCard
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.ui.viewmodel.VaultViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onNavigateChangePassword: () -> Unit,
    onNavigateStorageInfo: () -> Unit,
    onNavigateAboutPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    var showTimeoutMenu by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    val biometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VaultTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultSurface)
            )
        },
        containerColor = VaultBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Section
            SettingsSectionHeader("SECURITY & ACCESS")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.LockReset,
                        title = "Change Vault PIN",
                        subtitle = "Update your access PIN and master key wrapper",
                        onClick = onNavigateChangePassword
                    )

                    HorizontalDivider(color = VaultBorder)

                    // Biometrics switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(VaultAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = VaultAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(start = 14.dp)) {
                                Text(
                                    text = "Biometric Unlock",
                                    color = VaultTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (biometricAvailable) "Unlock with fingerprint or face" else "Not supported on this device",
                                    color = VaultTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = config?.biometricEnabled == true,
                            onCheckedChange = { enabled ->
                                viewModel.setBiometric(enabled)
                            },
                            enabled = biometricAvailable,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = VaultAccent,
                                uncheckedBorderColor = VaultBorder
                            )
                        )
                    }

                    HorizontalDivider(color = VaultBorder)

                    // Auto-Lock Timeout
                    Box {
                        SettingsItem(
                            icon = Icons.Default.LockClock,
                            title = "Auto-Lock Timeout",
                            subtitle = formatTimeout(config?.autoLockTimeoutSeconds ?: 30),
                            onClick = { showTimeoutMenu = true }
                        )

                        DropdownMenu(
                            expanded = showTimeoutMenu,
                            onDismissRequest = { showTimeoutMenu = false },
                            modifier = Modifier.background(VaultSurfaceCard)
                        ) {
                            val timeouts = listOf(
                                Pair(0, "Immediately on exit"),
                                Pair(30, "30 seconds"),
                                Pair(60, "1 minute"),
                                Pair(300, "5 minutes"),
                                Pair(900, "15 minutes")
                            )
                            timeouts.forEach { (seconds, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            color = if (config?.autoLockTimeoutSeconds == seconds) VaultPrimary else VaultTextPrimary
                                        )
                                    },
                                    onClick = {
                                        viewModel.setAutoLockTimeout(seconds)
                                        showTimeoutMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Storage & Details Section
            SettingsSectionHeader("DATA & PRIVACY")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.PieChart,
                        title = "Storage Information",
                        subtitle = "View encrypted size and file breakdown",
                        onClick = onNavigateStorageInfo
                    )

                    HorizontalDivider(color = VaultBorder)

                    SettingsItem(
                        icon = Icons.Default.Shield,
                        title = "Security & Privacy Policy",
                        subtitle = "Threat model and encryption specifications",
                        onClick = onNavigateAboutPrivacy
                    )
                }
            }

            // Danger Zone
            SettingsSectionHeader("DANGER ZONE")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultError.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    iconTint = VaultError,
                    title = "Delete All Vault Data",
                    titleColor = VaultError,
                    subtitle = "Permanently wipes all encrypted media, albums, and resets PIN",
                    onClick = { showWipeConfirmDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showWipeConfirmDialog) {
        ConfirmDeleteDialog(
            title = "WIPE ENTIRE VAULT?",
            message = "DANGER: This will permanently delete ALL encrypted photos, videos, albums, and master encryption keys. This operation is IRREVERSIBLE. Are you completely sure?",
            confirmText = "Wipe Everything",
            onDismiss = { showWipeConfirmDialog = false },
            onConfirm = {
                viewModel.wipeAllVaultData(context) {
                    showWipeConfirmDialog = false
                    Toast.makeText(context, "All vault data permanently erased", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = VaultTextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = VaultPrimary,
    title: String,
    titleColor: Color = VaultTextPrimary,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = VaultTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = VaultTextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

private fun formatTimeout(seconds: Int): String {
    return when (seconds) {
        0 -> "Immediately on exit"
        30 -> "30 seconds"
        60 -> "1 minute"
        300 -> "5 minutes"
        900 -> "15 minutes"
        else -> "$seconds seconds"
    }
}
