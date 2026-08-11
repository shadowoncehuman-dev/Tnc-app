package com.tncnursing.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tncnursing.app.data.models.Session
import com.tncnursing.app.ui.components.SessionCard
import com.tncnursing.app.ui.theme.TncNavyPrimary
import com.tncnursing.app.ui.viewmodel.MainViewModel
import com.tncnursing.app.ui.viewmodel.UiState

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults

@Composable
fun EnotesScreen(
    viewModel: MainViewModel,
    onSessionClick: (Session) -> Unit
) {
    val sessionsState by viewModel.sessionsState.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val downloadedNotes by viewModel.downloadedNotes.collectAsState()
    val bookmarkedIds = bookmarks.map { it.targetId }.toSet()

    var searchQuery by remember { mutableStateOf("") }
    var showOnlyDownloaded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "E-Notes & Study PDFs",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search PDF notes by subject or topic...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Offline / Downloaded Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showOnlyDownloaded,
                onClick = { showOnlyDownloaded = false },
                label = { Text("All E-Notes") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TncNavyPrimary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            FilterChip(
                selected = showOnlyDownloaded,
                onClick = { showOnlyDownloaded = true },
                label = { Text("Downloaded Offline (${downloadedNotes.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TncNavyPrimary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (val state = sessionsState) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = TncNavyPrimary)
                }
            }
            is UiState.Success -> {
                val pdfSessions = state.data.filter { session ->
                    val isPdf = session.type == "pdf" || session.pdfUrl != null || session.contentType == "pdf"
                    val matchesQuery = searchQuery.isBlank() || session.title.contains(searchQuery, ignoreCase = true)
                    val matchesDownload = !showOnlyDownloaded || downloadedNotes.contains(session.rowId)
                    isPdf && matchesQuery && matchesDownload
                }

                if (pdfSessions.isEmpty()) {
                    Text(
                        text = if (showOnlyDownloaded) "No offline downloaded notes yet. Tap the download icon on any note to save it!" else "No PDF E-notes found.",
                        modifier = Modifier.padding(top = 24.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(pdfSessions) { session ->
                            val isDownloaded = downloadedNotes.contains(session.rowId)
                            SessionCard(
                                session = session,
                                isBookmarked = bookmarkedIds.contains(session.rowId),
                                isDownloaded = isDownloaded,
                                onDownloadClick = { viewModel.downloadNote(session) },
                                onSessionClick = { onSessionClick(session) },
                                onBookmarkToggle = {
                                    viewModel.toggleBookmark(
                                        targetId = session.rowId,
                                        type = "pdf",
                                        title = session.title,
                                        subtitle = session.duration ?: ""
                                    )
                                }
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Text("Failed to load PDF notes.")
            }
        }
    }
}
