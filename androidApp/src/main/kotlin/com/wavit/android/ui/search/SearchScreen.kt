package com.wavit.android.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wavit.models.Song

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onSongSelected: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pinned Top Search Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search songs or videos on InnerTube...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = viewModel::clearSearch) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.onSearch()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Results Section
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.errorMessage != null -> {
                        Text(
                            text = "Error: ${uiState.errorMessage}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        )
                    }

                    uiState.hasSearched && uiState.songs.isEmpty() -> {
                        Text(
                            text = "No songs found for \"${uiState.query}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        )
                    }

                    !uiState.hasSearched -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Search for a song to test InnerTube playback",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                items = uiState.songs,
                                key = { it.videoId }
                            ) { song ->
                                SongResultItem(
                                    song = song,
                                    onClick = { onSongSelected(song) }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Auto-focus search bar and open keyboard on first display
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
fun SongResultItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail artwork
        if (!song.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Surface(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration text
        song.durationText?.let { duration ->
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

