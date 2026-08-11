package com.tncnursing.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tncnursing.app.data.models.Session
import com.tncnursing.app.ui.theme.TncAmberSecondary
import com.tncnursing.app.ui.theme.TncNavyPrimary

@Composable
fun SessionCard(
    session: Session,
    isBookmarked: Boolean,
    onSessionClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    isDownloaded: Boolean = false,
    onDownloadClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isPdf = session.type == "pdf" || session.pdfUrl != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSessionClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPdf) Color(0xFFFEF3C7) else Color(0xFFE0E7FF),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPdf) Icons.Default.Description else Icons.Default.PlayArrow,
                        contentDescription = if (isPdf) "PDF Note" else "Play Video",
                        tint = if (isPdf) Color(0xFFD97706) else TncNavyPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (session.serialNo.isNotBlank()) {
                        Text(
                            text = "Lec ${session.serialNo}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TncNavyPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPdf) Color(0xFFFFFBEB) else Color(0xFFEEF2FF)
                    ) {
                        Text(
                            text = if (isPdf) "PDF E-NOTE" else "VIDEO LECTURE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPdf) Color(0xFFB45309) else Color(0xFF3730A3),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "OFFLINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (session.duration != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.duration,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (isPdf && onDownloadClick != null) {
                IconButton(onClick = onDownloadClick) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                        contentDescription = "Download E-Note",
                        tint = if (isDownloaded) Color(0xFF166534) else TncNavyPrimary
                    )
                }
            }

            IconButton(onClick = onBookmarkToggle) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark Session",
                    tint = if (isBookmarked) TncAmberSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
