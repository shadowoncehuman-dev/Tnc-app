package com.tncnursing.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tncnursing.app.data.models.Session
import com.tncnursing.app.ui.components.SessionCard
import com.tncnursing.app.ui.theme.TncAmberSecondary
import com.tncnursing.app.ui.theme.TncNavyPrimary
import com.tncnursing.app.ui.viewmodel.MainViewModel
import com.tncnursing.app.ui.viewmodel.UiState

@Composable
fun CourseDetailScreen(
    courseRowId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onSessionClick: (Session) -> Unit
) {
    LaunchedEffect(courseRowId) {
        viewModel.loadCourseDetail(courseRowId)
    }

    val course by viewModel.selectedCourse.collectAsState()
    val sessionsState by viewModel.sessionsState.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val bookmarkedIds = bookmarks.map { it.targetId }.toSet()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("All Sessions", "Video Lectures", "E-Notes / PDFs")

    val activeCourse = course

    if (activeCourse == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TncNavyPrimary)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Course Banner Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(activeCourse.imageUrl ?: "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800")
                    .crossfade(true)
                    .build(),
                contentDescription = activeCourse.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )

            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = activeCourse.category,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TncAmberSecondary
                )
                Text(
                    text = activeCourse.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Session List
        when (val state = sessionsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TncNavyPrimary)
                }
            }
            is UiState.Success -> {
                val sessions = state.data
                val filtered = when (selectedTabIndex) {
                    1 -> sessions.filter { it.type == "video" || it.videoUrl != null }
                    2 -> sessions.filter { it.type == "pdf" || it.pdfUrl != null }
                    else -> sessions
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No sessions available in this tab.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(filtered) { session ->
                            SessionCard(
                                session = session,
                                isBookmarked = bookmarkedIds.contains(session.rowId),
                                onSessionClick = { onSessionClick(session) },
                                onBookmarkToggle = {
                                    viewModel.toggleBookmark(
                                        targetId = session.rowId,
                                        type = session.type,
                                        title = session.title,
                                        subtitle = session.duration ?: ""
                                    )
                                },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error loading sessions.")
                }
            }
        }
    }
}
