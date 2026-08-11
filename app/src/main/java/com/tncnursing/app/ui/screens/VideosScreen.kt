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

@Composable
fun VideosScreen(
    viewModel: MainViewModel,
    onSessionClick: (Session) -> Unit
) {
    val sessionsState by viewModel.sessionsState.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val bookmarkedIds = bookmarks.map { it.targetId }.toSet()

    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Video Lectures",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search video lectures by topic...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                val videoSessions = state.data.filter { session ->
                    val isVideo = session.type == "video" || session.videoUrl != null || session.contentType == "youtube"
                    val matchesQuery = searchQuery.isBlank() || session.title.contains(searchQuery, ignoreCase = true)
                    isVideo && matchesQuery
                }

                if (videoSessions.isEmpty()) {
                    Text("No video lectures found.", modifier = Modifier.padding(top = 24.dp))
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(videoSessions) { session ->
                            SessionCard(
                                session = session,
                                isBookmarked = bookmarkedIds.contains(session.rowId),
                                onSessionClick = { onSessionClick(session) },
                                onBookmarkToggle = {
                                    viewModel.toggleBookmark(
                                        targetId = session.rowId,
                                        type = "video",
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
                Text("Failed to load video lectures.")
            }
        }
    }
}
