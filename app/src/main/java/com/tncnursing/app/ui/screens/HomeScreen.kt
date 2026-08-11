package com.tncnursing.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tncnursing.app.data.models.Course
import com.tncnursing.app.data.models.Session
import com.tncnursing.app.data.models.SliderItem
import com.tncnursing.app.ui.components.CourseCard
import com.tncnursing.app.ui.components.SessionCard
import com.tncnursing.app.ui.theme.TncAmberSecondary
import com.tncnursing.app.ui.theme.TncBlueContainer
import com.tncnursing.app.ui.theme.TncNavyDark
import com.tncnursing.app.ui.theme.TncNavyPrimary
import com.tncnursing.app.ui.viewmodel.MainViewModel
import com.tncnursing.app.ui.viewmodel.UiState

import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToCourses: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToEnotes: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onCourseClick: (String) -> Unit,
    onSessionClick: (Session) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val coursesState by viewModel.coursesState.collectAsState()
    val sessionsState by viewModel.sessionsState.collectAsState()
    val sliders by viewModel.slidersState.collectAsState()
    val recentProgress by viewModel.recentProgress.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val downloadedNotes by viewModel.downloadedNotes.collectAsState()

    val bookmarkedIds = bookmarks.map { it.targetId }.toSet()
    var homeSearchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Welcome Header Banner & Search Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(TncNavyPrimary, TncNavyDark)
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Welcome back, ${userProfile?.name ?: "Learner"}!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target: ${userProfile?.targetExam ?: "NORCET 8.0 AIIMS 2026"}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TncAmberSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search bar at top of Home page
                    OutlinedTextField(
                        value = homeSearchQuery,
                        onValueChange = { homeSearchQuery = it },
                        placeholder = { Text("Search batches, video lectures, topics...", color = Color.LightGray) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TncAmberSecondary) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TncAmberSecondary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sliders / Announcement Banner
                    if (sliders.isNotEmpty()) {
                        val firstSlider = sliders.first()
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(firstSlider.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = firstSlider.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = firstSlider.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = firstSlider.description,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Action Shortcut Grid
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Study Access",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionTile("Target Batches", Icons.Default.Book, Color(0xFFE0E7FF), Color(0xFF3730A3), onNavigateToCourses)
                    QuickActionTile("Video Lectures", Icons.Default.PlayCircle, Color(0xFFDCFCE7), Color(0xFF166534), onNavigateToVideos)
                    QuickActionTile("E-Notes / PDFs", Icons.Default.Description, Color(0xFFFEF3C7), Color(0xFF92400E), onNavigateToEnotes)
                    QuickActionTile("Mock Tests", Icons.Default.Quiz, Color(0xFFFEE2E2), Color(0xFF991B1B), onNavigateToQuizzes)
                }
            }
        }

        // 3. Recently Viewed Section (Displays last 5 accessed videos or e-notes)
        if (recentProgress.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Recently Viewed (Last 5 Accessed)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val last5 = recentProgress.take(5)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(last5) { recent ->
                            Card(
                                modifier = Modifier
                                    .width(240.dp)
                                    .clickable {
                                        if (recent.type == "video") {
                                            onSessionClick(Session(rowId = recent.targetId, title = recent.title, type = "video"))
                                        } else {
                                            onSessionClick(Session(rowId = recent.targetId, title = recent.title, type = "pdf", pdfUrl = "sample"))
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (recent.type == "video") Icons.Default.PlayCircle else Icons.Default.Description,
                                        contentDescription = "Recently Viewed",
                                        tint = if (recent.type == "video") TncNavyPrimary else Color(0xFFD97706),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recent.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (recent.type == "video") "Video Lecture" else "PDF E-Note",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Featured Target Batches
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Featured Study Batches",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "View All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TncNavyPrimary,
                        modifier = Modifier.clickable { onNavigateToCourses() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (val state = coursesState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TncNavyPrimary)
                        }
                    }
                    is UiState.Success -> {
                        val filteredCourses = state.data.filter { course ->
                            homeSearchQuery.isBlank() ||
                                course.name.contains(homeSearchQuery, ignoreCase = true) ||
                                course.description.contains(homeSearchQuery, ignoreCase = true) ||
                                course.category.contains(homeSearchQuery, ignoreCase = true)
                        }

                        if (filteredCourses.isEmpty()) {
                            Text("No study batches matching search.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredCourses.take(6)) { course ->
                                    CourseCard(
                                        course = course,
                                        isBookmarked = bookmarkedIds.contains(course.rowId),
                                        onCourseClick = { onCourseClick(course.rowId) },
                                        onBookmarkToggle = {
                                            viewModel.toggleBookmark(
                                                targetId = course.rowId,
                                                type = "course",
                                                title = course.name,
                                                subtitle = course.category,
                                                imageUrl = course.imageUrl
                                            )
                                        },
                                        modifier = Modifier.width(260.dp)
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error -> {
                        Text("Unable to load courses", color = Color.Red, fontSize = 13.sp)
                    }
                }
            }
        }

        // 5. Daily Challenge Quiz Card
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clickable { onNavigateToQuizzes() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Daily Quiz",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Practice Test (+20 XP)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "10 high-yield nursing questions with negative marking & explanations.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Start",
                        tint = Color(0xFF92400E)
                    )
                }
            }
        }

        // 6. Recent Video Lectures / Notes
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Lectures & Notes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Explore All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TncNavyPrimary,
                        modifier = Modifier.clickable { onNavigateToVideos() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (val state = sessionsState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(color = TncNavyPrimary)
                    }
                    is UiState.Success -> {
                        val filteredSessions = state.data.filter { session ->
                            homeSearchQuery.isBlank() ||
                                session.title.contains(homeSearchQuery, ignoreCase = true) ||
                                (session.duration?.contains(homeSearchQuery, ignoreCase = true) == true)
                        }

                        if (filteredSessions.isEmpty()) {
                            Text("No lectures or notes found matching search.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        } else {
                            filteredSessions.take(5).forEach { session ->
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
                                            type = session.type,
                                            title = session.title,
                                            subtitle = session.duration ?: ""
                                        )
                                    },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        Text("Unable to load recent sessions", color = Color.Red, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
