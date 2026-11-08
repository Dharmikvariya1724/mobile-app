package com.example.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VaultAccent
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultGold
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceCard
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
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
            // Version & Identity Header
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(VaultPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = VaultPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "PrivateVault v1.0",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                    Text(
                        text = "Zero-Knowledge Local Storage",
                        fontSize = 13.sp,
                        color = VaultAccent
                    )
                }
            }

            // Cryptographic Specifications
            Text(
                text = "CRYPTOGRAPHIC ARCHITECTURE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextMuted,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SpecItem(
                        icon = Icons.Default.Lock,
                        title = "AES-256-GCM Authenticated Encryption",
                        desc = "All photos and videos are encrypted using Galois/Counter Mode (AES-GCM) with 256-bit keys and 128-bit authentication tags. Tampering with ciphertext is immediately detected."
                    )
                    HorizontalDivider(color = VaultBorder)
                    SpecItem(
                        icon = Icons.Default.Key,
                        title = "PBKDF2 Key Derivation (120,000 Iterations)",
                        desc = "Your PIN/password derives a 256-bit Key Encryption Key (KEK) using PBKDF2 with HmacSHA256, 120,000 hash iterations, and a cryptographically secure 32-byte salt."
                    )
                    HorizontalDivider(color = VaultBorder)
                    SpecItem(
                        icon = Icons.Default.CloudOff,
                        title = "100% Offline & Backup Excluded",
                        desc = "No network permission is declared in the app manifest. All vault files and databases are explicitly excluded from Android Google Drive cloud backups and device transfers."
                    )
                }
            }

            // Threat Model & Limitations
            Text(
                text = "THREAT MODEL & LIMITATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextMuted,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThreatItem(
                        title = "Compromised Operating Systems & Root",
                        desc = "On rooted or jailbroken devices, kernel-level adversaries may bypass sandbox protections or inspect RAM. For maximum protection, run on unmodified Android devices."
                    )
                    HorizontalDivider(color = VaultBorder)
                    ThreatItem(
                        title = "Loss of PIN / Password",
                        desc = "PrivateVault does not maintain backdoors or recovery servers. If you forget your PIN, encrypted data cannot be decrypted by anyone."
                    )
                    HorizontalDivider(color = VaultBorder)
                    ThreatItem(
                        title = "Original Public Gallery Copies",
                        desc = "Importing media encrypts a copy in PrivateVault. Cloud backups (e.g., Google Photos, OneDrive) or other apps with public gallery access may still have copies until you delete them."
                    )
                    HorizontalDivider(color = VaultBorder)
                    ThreatItem(
                        title = "FLAG_SECURE Protections",
                        desc = "Screenshots and task switch previews are blocked by the OS via FLAG_SECURE. However, physical cameras recording the screen cannot be prevented."
                    )
                }
            }

            // Privacy Policy Summary
            Text(
                text = "PRIVACY POLICY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextMuted,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Collection: None. PrivateVault does not collect, log, transmit, or monetize any user data, media, telemetry, or device identifiers.\n\n" +
                                "Permissions: Only uses internal app storage and standard zero-permission Photo Picker. Biometrics are verified strictly on-device using Android BiometricPrompt.\n\n" +
                                "Sharing: Media is only exported when explicitly chosen by you.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SpecItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VaultPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VaultPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = title, color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = desc, color = VaultTextSecondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun ThreatItem(title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = VaultGold,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = title, color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = desc, color = VaultTextSecondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
