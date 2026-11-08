package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurfaceCard
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun PinDotsIndicator(
    length: Int,
    enteredCount: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until length) {
            val isFilled = i < enteredCount
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> MaterialTheme.colorScheme.error
                    isFilled -> VaultPrimary
                    else -> VaultBorder
                },
                label = "dot_color"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("BIOMETRIC", "0", "BACKSPACE")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "BIOMETRIC" -> {
                            if (onBiometricClick != null) {
                                KeypadIconButton(
                                    testTag = "keypad_biometric",
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Unlock with Biometrics",
                                            tint = VaultPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    },
                                    onClick = onBiometricClick
                                )
                            } else {
                                Spacer(modifier = Modifier.size(72.dp))
                            }
                        }
                        "BACKSPACE" -> {
                            KeypadIconButton(
                                testTag = "keypad_backspace",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Delete last digit",
                                        tint = VaultTextSecondary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                onClick = onDeleteClick
                            )
                        }
                        else -> {
                            KeypadDigitButton(
                                digit = key,
                                onClick = { onDigitClick(key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .testTag("keypad_digit_$digit")
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 36.dp),
                onClick = onClick
            ),
        shape = CircleShape,
        color = VaultSurfaceCard,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = VaultTextPrimary
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    testTag: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .testTag(testTag)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 36.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
