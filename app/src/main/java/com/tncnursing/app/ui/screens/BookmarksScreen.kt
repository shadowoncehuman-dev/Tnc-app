package com.tncnursing.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tncnursing.app.data.models.Session
import com.tncnursing.app.ui.theme.TncAmberSecondary
import com.tncnursing.app.ui.theme.TncNavyPrimary
import com.tncnursing.app.ui.viewmodel.MainViewModel

@Composable
fun BookmarksScreen(
    viewModel: MainViewModel,
    onCourseClick: (String) -> Unit,
    onSessionClick: (Session) -> Unit
) {
    val bookmarks by viewModel.bookmarks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = "Bookmarks",
                tint = TncAmberSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Saved Bookmarks",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (bookmarks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Empty",
                    tint = Color.LightGray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "No saved bookmarks yet",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = "Tap the bookmark icon on any course, lecture, or E-note to save for quick access offline.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(bookmarks) { b ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (b.type == "course") {
                                    onCourseClick(b.targetId)
                                } else if (b.type == "pdf") {
                                    onSessionClick(Session(rowId = b.targetId, title = b.title, type = "pdf", pdfUrl = "sample"))
                                } else {
                                    onSessionClick(Session(rowId = b.targetId, title = b.title, type = "video"))
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when (b.type) {
                                "course" -> Icons.Default.Book
                                "pdf" -> Icons.Default.Description
                                else -> Icons.Default.PlayCircle
                            }

                            val iconColor = when (b.type) {
                                "course" -> TncNavyPrimary
                                "pdf" -> Color(0xFFD97706)
                                else -> Color(0xFF059669)
                            }

                            Icon(imageVector = icon, contentDescription = b.type, tint = iconColor)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = iconColor.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = b.type.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iconColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = b.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (b.subtitle.isNotBlank()) {
                                    Text(
                                        text = b.subtitle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.toggleBookmark(b.targetId, b.type, b.title) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color.Red.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
