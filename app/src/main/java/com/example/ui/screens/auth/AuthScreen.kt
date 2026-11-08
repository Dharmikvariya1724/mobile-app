package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.security.BiometricHelper
import com.example.ui.components.PinDotsIndicator
import com.example.ui.components.PinKeypad
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

private enum class SetupStep {
    WELCOME,
    CREATE_PIN,
    CONFIRM_PIN,
    BIOMETRIC_OFFER
}

@Composable
fun AuthScreen(
    viewModel: VaultViewModel,
    onAuthenticated: () -> Unit
) {
    val isInitialized by viewModel.isInitialized.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val lockoutSeconds by viewModel.lockoutSeconds.collectAsState()
    val config by viewModel.config.collectAsState()

    var setupStep by remember { mutableStateOf(SetupStep.WELCOME) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var setupError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val pinLength = 4

    // If vault initialization state is loading
    if (isInitialized == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VaultBackground),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(color = VaultPrimary)
        }
    } else if (isInitialized == false) {
        when (setupStep) {
            SetupStep.WELCOME -> {
                WelcomeView(onGetStarted = { setupStep = SetupStep.CREATE_PIN })
            }
            SetupStep.CREATE_PIN -> {
                PinEntryView(
                    title = "Create Vault PIN",
                    subtitle = "Choose a 4-digit PIN to secure your private media.",
                    pinLength = pinLength,
                    currentPin = enteredPin,
                    error = setupError,
                    onDigit = { digit ->
                        if (enteredPin.length < pinLength) {
                            enteredPin += digit
                            setupError = null
                            if (enteredPin.length == pinLength) {
                                setupStep = SetupStep.CONFIRM_PIN
                            }
                        }
                    },
                    onDelete = {
                        if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                    }
                )
            }
            SetupStep.CONFIRM_PIN -> {
                PinEntryView(
                    title = "Confirm Vault PIN",
                    subtitle = "Re-enter your 4-digit PIN to confirm.",
                    pinLength = pinLength,
                    currentPin = confirmedPin,
                    error = setupError,
                    onDigit = { digit ->
                        if (confirmedPin.length < pinLength) {
                            confirmedPin += digit
                            setupError = null
                            if (confirmedPin.length == pinLength) {
                                if (confirmedPin == enteredPin) {
                                    // PIN matched! Check biometrics availability
                                    if (BiometricHelper.isBiometricAvailable(context)) {
                                        setupStep = SetupStep.BIOMETRIC_OFFER
                                    } else {
                                        viewModel.setupPassword(enteredPin) {
                                            onAuthenticated()
                                        }
                                    }
                                } else {
                                    setupError = "PINs do not match. Please try again."
                                    confirmedPin = ""
                                }
                            }
                        }
                    },
                    onDelete = {
                        if (confirmedPin.isNotEmpty()) confirmedPin = confirmedPin.dropLast(1)
                    }
                )
            }
            SetupStep.BIOMETRIC_OFFER -> {
                BiometricSetupView(
                    onEnable = {
                        viewModel.setupPassword(enteredPin) {
                            viewModel.setBiometric(true)
                            onAuthenticated()
                        }
                    },
                    onSkip = {
                        viewModel.setupPassword(enteredPin) {
                            viewModel.setBiometric(false)
                            onAuthenticated()
                        }
                    }
                )
            }
        }
    } else {
        // Vault is initialized -> Show Login Screen
        var loginPin by remember { mutableStateOf("") }

        LaunchedEffect(config?.biometricEnabled) {
            if (config?.biometricEnabled == true && BiometricHelper.isBiometricAvailable(context)) {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            viewModel.loginBiometric {
                                onAuthenticated()
                            }
                        },
                        onError = {}
                    )
                }
            }
        }

        PinEntryView(
            title = "Unlock Private Vault",
            subtitle = if (lockoutSeconds > 0) "Too many attempts" else "Enter your PIN to access encrypted files.",
            pinLength = pinLength,
            currentPin = loginPin,
            error = authError,
            lockoutSeconds = lockoutSeconds,
            showBiometric = config?.biometricEnabled == true && BiometricHelper.isBiometricAvailable(context),
            onBiometricClick = {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            viewModel.loginBiometric {
                                onAuthenticated()
                            }
                        },
                        onError = { /* handled */ }
                    )
                }
            },
            onDigit = { digit ->
                if (lockoutSeconds <= 0 && loginPin.length < pinLength) {
                    loginPin += digit
                    if (loginPin.length == pinLength) {
                        viewModel.login(loginPin)
                        loginPin = ""
                    }
                }
            },
            onDelete = {
                if (loginPin.isNotEmpty()) loginPin = loginPin.dropLast(1)
            }
        )
    }
}

@Composable
private fun WelcomeView(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(VaultPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield",
                    tint = VaultPrimary,
                    modifier = Modifier.size(54.dp)
                )
            }

            Text(
                text = "PrivateVault",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )

            Text(
                text = "Hardware-grade AES-256 encryption for your photos and videos. 100% offline and device-bound.",
                fontSize = 15.sp,
                color = VaultTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Feature cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SecurityFeatureCard(
                icon = Icons.Default.Lock,
                title = "AES-256-GCM Encryption",
                desc = "Media is encrypted chunk-by-chunk using authenticated AES-256."
            )
            SecurityFeatureCard(
                icon = Icons.Default.Security,
                title = "Zero Cloud & Zero Tracking",
                desc = "No servers, no tracking SDKs, no ads. Files never leave your device."
            )
            SecurityFeatureCard(
                icon = Icons.Default.Fingerprint,
                title = "Biometric & FLAG_SECURE",
                desc = "Instant biometric unlock with screenshot & screen recording blocking."
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Loss warning callout
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = VaultGold.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = VaultGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Important: Because this vault is zero-knowledge and offline, there is no password reset via email. Keep your PIN safe.",
                    color = VaultGold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGetStarted,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VaultPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("welcome_get_started_button")
        ) {
            Text(
                text = "Create My Private Vault",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = VaultSurface
            )
        }
    }
}

@Composable
private fun SecurityFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VaultPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VaultPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = title,
                    color = VaultTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = desc,
                    color = VaultTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PinEntryView(
    title: String,
    subtitle: String,
    pinLength: Int,
    currentPin: String,
    error: String? = null,
    lockoutSeconds: Long = 0L,
    showBiometric: Boolean = false,
    onBiometricClick: (() -> Unit)? = null,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(VaultPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = VaultPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = VaultTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            PinDotsIndicator(
                length = pinLength,
                enteredCount = currentPin.length,
                isError = error != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = error ?: "",
                    color = VaultError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        PinKeypad(
            onDigitClick = onDigit,
            onDeleteClick = onDelete,
            onBiometricClick = if (showBiometric) onBiometricClick else null
        )
    }
}

@Composable
private fun BiometricSetupView(
    onEnable: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(VaultAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometrics",
                    tint = VaultAccent,
                    modifier = Modifier.size(54.dp)
                )
            }

            Text(
                text = "Enable Biometric Unlock",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )

            Text(
                text = "Unlock your private vault faster using your fingerprint or facial recognition without compromising encryption security.",
                fontSize = 14.sp,
                color = VaultTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onEnable,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VaultPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("enable_biometrics_button")
            ) {
                Text("Enable Biometric Unlock", color = VaultSurface, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onSkip,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("skip_biometrics_button")
            ) {
                Text("Skip for Now", color = VaultTextSecondary)
            }
        }
    }
}
