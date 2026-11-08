package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PinDotsIndicator
import com.example.ui.components.PinKeypad
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultError
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.ui.viewmodel.VaultViewModel

private enum class ChangeStep {
    CURRENT_PIN,
    NEW_PIN,
    CONFIRM_NEW_PIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pinLength = 4

    var currentStep by remember { mutableStateOf(ChangeStep.CURRENT_PIN) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val enteredDigits = when (currentStep) {
        ChangeStep.CURRENT_PIN -> currentPin
        ChangeStep.NEW_PIN -> newPin
        ChangeStep.CONFIRM_NEW_PIN -> confirmNewPin
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Vault PIN", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(VaultPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = VaultPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (currentStep) {
                        ChangeStep.CURRENT_PIN -> "Enter Current PIN"
                        ChangeStep.NEW_PIN -> "Enter New PIN"
                        ChangeStep.CONFIRM_NEW_PIN -> "Confirm New PIN"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (currentStep) {
                        ChangeStep.CURRENT_PIN -> "Authenticate with your existing PIN."
                        ChangeStep.NEW_PIN -> "Choose a new 4-digit PIN."
                        ChangeStep.CONFIRM_NEW_PIN -> "Re-enter your new PIN to confirm."
                    },
                    fontSize = 14.sp,
                    color = VaultTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                PinDotsIndicator(
                    length = pinLength,
                    enteredCount = enteredDigits.length,
                    isError = errorMessage != null
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = VaultError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            PinKeypad(
                onDigitClick = { digit ->
                    errorMessage = null
                    when (currentStep) {
                        ChangeStep.CURRENT_PIN -> {
                            if (currentPin.length < pinLength) {
                                currentPin += digit
                                if (currentPin.length == pinLength) {
                                    currentStep = ChangeStep.NEW_PIN
                                }
                            }
                        }
                        ChangeStep.NEW_PIN -> {
                            if (newPin.length < pinLength) {
                                newPin += digit
                                if (newPin.length == pinLength) {
                                    currentStep = ChangeStep.CONFIRM_NEW_PIN
                                }
                            }
                        }
                        ChangeStep.CONFIRM_NEW_PIN -> {
                            if (confirmNewPin.length < pinLength) {
                                confirmNewPin += digit
                                if (confirmNewPin.length == pinLength) {
                                    if (confirmNewPin == newPin) {
                                        viewModel.changePassword(
                                            currentPass = currentPin,
                                            newPass = newPin,
                                            onSuccess = {
                                                Toast.makeText(context, "PIN updated successfully", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            },
                                            onError = { err ->
                                                errorMessage = err
                                                currentStep = ChangeStep.CURRENT_PIN
                                                currentPin = ""
                                                newPin = ""
                                                confirmNewPin = ""
                                            }
                                        )
                                    } else {
                                        errorMessage = "New PINs do not match. Try again."
                                        confirmNewPin = ""
                                    }
                                }
                            }
                        }
                    }
                },
                onDeleteClick = {
                    when (currentStep) {
                        ChangeStep.CURRENT_PIN -> if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1)
                        ChangeStep.NEW_PIN -> if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
                        ChangeStep.CONFIRM_NEW_PIN -> if (confirmNewPin.isNotEmpty()) confirmNewPin = confirmNewPin.dropLast(1)
                    }
                }
            )
        }
    }
}
