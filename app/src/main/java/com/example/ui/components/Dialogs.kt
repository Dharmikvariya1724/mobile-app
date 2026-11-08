package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaEntity
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultError
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceCard
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreateAlbumDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var albumName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Album", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Enter a name for your private album.",
                    color = VaultTextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text("Album Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultPrimary,
                        unfocusedBorderColor = VaultBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("album_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (albumName.isNotBlank()) {
                        onConfirm(albumName)
                    }
                },
                enabled = albumName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VaultPrimary),
                modifier = Modifier.testTag("create_album_confirm")
            ) {
                Text("Create", color = VaultSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        },
        containerColor = VaultSurfaceCard
    )
}

@Composable
fun MoveToAlbumDialog(
    albums: List<AlbumEntity>,
    onDismiss: () -> Unit,
    onSelectAlbum: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Album", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Select an album destination for selected items:",
                    color = VaultTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAlbum(null) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderSpecial,
                                contentDescription = null,
                                tint = VaultTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "None (Remove from albums)",
                                color = VaultTextPrimary,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        HorizontalDivider(color = VaultBorder)
                    }

                    items(albums) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAlbum(album.id) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = VaultPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = album.name,
                                color = VaultTextPrimary,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        HorizontalDivider(color = VaultBorder)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        },
        containerColor = VaultSurfaceCard
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String = "Delete Media?",
    message: String = "Are you sure you want to permanently delete this item from your private vault? This cannot be undone.",
    confirmText: String = "Delete",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = VaultError,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text(title, color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(message, color = VaultTextSecondary, fontSize = 14.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = VaultError),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(confirmText, color = androidx.compose.ui.graphics.Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        },
        containerColor = VaultSurfaceCard
    )
}

@Composable
fun MediaDetailsDialog(
    media: MediaEntity,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = VaultPrimary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text("Media Details", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Name", media.originalDisplayName)
                DetailRow("Type", media.mediaType)
                DetailRow("MIME", media.mimeType)
                DetailRow("File Size", formatFileSize(media.fileSize))
                if (media.width > 0 && media.height > 0) {
                    DetailRow("Resolution", "${media.width} x ${media.height}")
                }
                if (media.durationMs > 0) {
                    DetailRow("Duration", formatDuration(media.durationMs))
                }
                DetailRow("Imported", dateFormat.format(Date(media.importedAt)))
                DetailRow("Encrypted ID", media.uuid.take(8) + "...")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VaultPrimary)
            ) {
                Text("Close", color = VaultSurface)
            }
        },
        containerColor = VaultSurfaceCard
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = VaultTextMuted, fontSize = 13.sp)
        Text(
            text = value,
            color = VaultTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
