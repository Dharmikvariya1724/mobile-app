package com.example.ui.screens.dashboard

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlbumEntity
import com.example.data.model.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.SortOption
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CreateAlbumDialog
import com.example.ui.components.MediaThumbnailView
import com.example.ui.components.MoveToAlbumDialog
import com.example.ui.theme.VaultAccent
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultError
import com.example.ui.theme.VaultPrimary
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceCard
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: VaultViewModel,
    onOpenPhoto: (MediaEntity) -> Unit,
    onOpenVideo: (MediaEntity) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val allMedia by viewModel.allMedia.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedIds by viewModel.selectedMediaIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    var showSearchBar by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var showMoveToAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedAlbumForFilter by remember { mutableStateOf<AlbumEntity?>(null) }

    // Multi-photo/video picker using standard Android Photo Picker (zero-permission)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaUris(context, uris, selectedAlbumForFilter?.id)
            Toast.makeText(context, "Encrypting ${uris.size} file(s)...", Toast.LENGTH_SHORT).show()
        }
    }

    // Filter media based on current tab
    val currentTabMedia = remember(allMedia, selectedTab, selectedAlbumForFilter) {
        when {
            selectedAlbumForFilter != null -> allMedia.filter { it.albumId == selectedAlbumForFilter?.id }
            selectedTab == 0 -> allMedia
            selectedTab == 1 -> allMedia.filter { it.mediaType == MediaType.PHOTO.name }
            selectedTab == 2 -> allMedia.filter { it.mediaType == MediaType.VIDEO.name }
            selectedTab == 3 -> allMedia // handled in Albums view
            selectedTab == 4 -> allMedia.filter { it.isFavorite }
            else -> allMedia
        }
    }

    val isSelectionMode = selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Selection Action Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedIds.size} Selected",
                            color = VaultTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Selection",
                                tint = VaultTextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll(currentTabMedia) }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = VaultTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultSurfaceCard)
                )
            } else {
                TopAppBar(
                    title = {
                        if (showSearchBar) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search encrypted files...", color = VaultTextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VaultPrimary,
                                    unfocusedBorderColor = VaultBorder,
                                    focusedTextColor = VaultTextPrimary,
                                    unfocusedTextColor = VaultTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_text_input")
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(VaultPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = VaultPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = if (selectedAlbumForFilter != null) selectedAlbumForFilter!!.name else "PrivateVault",
                                    color = VaultTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) viewModel.setSearchQuery("")
                        }) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = VaultTextPrimary
                            )
                        }

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = VaultTextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(VaultSurfaceCard)
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.displayName,
                                                color = if (sortOption == option) VaultPrimary else VaultTextPrimary
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { viewModel.lock() }) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = VaultPrimary
                            )
                        }

                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = VaultTextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultSurface)
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    containerColor = VaultPrimary,
                    contentColor = VaultSurface,
                    modifier = Modifier.testTag("import_media_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Import Photos & Videos",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                // Batch Action Bottom Bar
                Surface(
                    color = VaultSurfaceCard,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionButton(
                            icon = Icons.Default.DriveFileMove,
                            label = "Album",
                            onClick = { showMoveToAlbumDialog = true }
                        )
                        ActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            color = VaultError,
                            onClick = { showDeleteConfirmDialog = true }
                        )
                    }
                }
            }
        },
        containerColor = VaultBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Import progress bar
            if (importProgress != null) {
                val (curr, total) = importProgress!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurfaceCard)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Encrypting media: $curr of $total",
                            color = VaultPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(curr * 100) / total}%",
                            color = VaultTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { curr.toFloat() / total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = VaultPrimary,
                        trackColor = VaultBorder
                    )
                }
            }

            // Tabs Row
            if (selectedAlbumForFilter == null) {
                val tabTitles = listOf("All", "Photos", "Videos", "Albums", "Favorites")
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = VaultSurface,
                    contentColor = VaultPrimary,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = VaultPrimary
                        )
                    },
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) VaultPrimary else VaultTextSecondary
                                )
                            }
                        )
                    }
                }
            } else {
                // Showing single album contents
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Viewing: ${selectedAlbumForFilter!!.name}",
                        color = VaultTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Back to Albums",
                        color = VaultPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { selectedAlbumForFilter = null }
                    )
                }
            }

            // Content Area
            if (selectedTab == 3 && selectedAlbumForFilter == null) {
                // Albums View
                AlbumsGridView(
                    albums = albums,
                    onCreateAlbum = { showCreateAlbumDialog = true },
                    onSelectAlbum = { album -> selectedAlbumForFilter = album },
                    onDeleteAlbum = { album -> viewModel.deleteAlbum(context, album, false) }
                )
            } else {
                // Media Grid View
                if (currentTabMedia.isEmpty()) {
                    EmptyVaultView(
                        tabName = when (selectedTab) {
                            1 -> "Photos"
                            2 -> "Videos"
                            4 -> "Favorites"
                            else -> "Media"
                        },
                        onImportClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(currentTabMedia, key = { it.id }) { media ->
                            val isSelected = selectedIds.contains(media.id)
                            MediaThumbnailView(
                                media = media,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(media.id)
                                    } else {
                                        if (media.mediaType == MediaType.VIDEO.name) {
                                            onOpenVideo(media)
                                        } else {
                                            onOpenPhoto(media)
                                        }
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(media.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateAlbumDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateAlbumDialog = false },
            onConfirm = { name ->
                viewModel.createAlbum(name)
                showCreateAlbumDialog = false
            }
        )
    }

    if (showMoveToAlbumDialog) {
        MoveToAlbumDialog(
            albums = albums,
            onDismiss = { showMoveToAlbumDialog = false },
            onSelectAlbum = { albumId ->
                viewModel.moveSelectedToAlbum(albumId)
                showMoveToAlbumDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        ConfirmDeleteDialog(
            title = "Delete ${selectedIds.size} Item(s)?",
            message = "These items will be permanently erased from your encrypted vault. Decrypted recovery will be impossible.",
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                viewModel.deleteSelectedItems(context)
                showDeleteConfirmDialog = false
            }
        )
    }
}

@Composable
private fun AlbumsGridView(
    albums: List<AlbumEntity>,
    onCreateAlbum: () -> Unit,
    onSelectAlbum: (AlbumEntity) -> Unit,
    onDeleteAlbum: (AlbumEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Create new album tile
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                modifier = Modifier
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCreateAlbum)
                    .testTag("create_album_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(VaultPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Album",
                            tint = VaultPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "New Album",
                        color = VaultPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        items(albums, key = { it.id }) { album ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                modifier = Modifier
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectAlbum(album) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = VaultPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        IconButton(
                            onClick = { onDeleteAlbum(album) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Album",
                                tint = VaultTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = album.name,
                        color = VaultTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultView(
    tabName: String,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VaultSurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = VaultTextMuted,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No $tabName in Vault",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = VaultTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Import private photos and videos from your phone. They will be encrypted immediately and hidden from gallery.",
            fontSize = 13.sp,
            color = VaultTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.material3.Button(
            onClick = onImportClick,
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VaultPrimary),
            modifier = Modifier.testTag("empty_state_import_button")
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = VaultSurface,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Import Media Now",
                color = VaultSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color = VaultTextPrimary,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
