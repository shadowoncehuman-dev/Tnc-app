package com.tncnursing.app.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tncnursing.app.ui.theme.TncAmberSecondary
import com.tncnursing.app.ui.theme.TncGreenSuccess
import com.tncnursing.app.ui.theme.TncNavyDark
import com.tncnursing.app.ui.theme.TncNavyPrimary
import com.tncnursing.app.ui.viewmodel.MainViewModel

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.filled.Speed

@Composable
fun VideoPlayerScreen(
    sessionRowId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(sessionRowId) {
        viewModel.loadSessionDetail(sessionRowId)
    }

    val session by viewModel.selectedSession.collectAsState()
    var isCompleted by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableStateOf("1.0x") }

    val activeSession = session

    LaunchedEffect(activeSession) {
        activeSession?.let {
            viewModel.recordProgress(
                targetId = it.rowId,
                type = "video",
                courseId = it.courseId ?: "",
                title = it.title,
                subtitle = "Video Lecture",
                progressSec = 60,
                totalSec = 1800,
                completed = false
            )
        }
    }

    val videoUrlToLoad = remember(activeSession, selectedSpeed) {
        val speedVal = selectedSpeed.replace("x", "")
        if (activeSession?.contentType == "fs" || activeSession?.videoUrl?.contains("videoplay") == true) {
            "https://videoplay.tncnursing.in/videos/fs/index.html?speed=$speedVal"
        } else {
            val rawUrl = activeSession?.videoUrl ?: ""
            val videoId = when {
                rawUrl.contains("v=") -> rawUrl.substringAfter("v=").substringBefore("&")
                rawUrl.contains("youtu.be/") -> rawUrl.substringAfter("youtu.be/").substringBefore("?")
                else -> "dQw4w9WgXcQ"
            }
            "https://www.youtube.com/embed/$videoId?autoplay=1&enablejsapi=1"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Player Header TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = activeSession?.title ?: "Video Lecture",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }

        // Video Viewport Area (Embedded Player Container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <body style="margin:0;padding:0;background-color:black;">
                                <div style="position:relative;padding-bottom:56.25%;height:0;overflow:hidden;">
                                    <iframe src="$videoUrlToLoad" 
                                            style="position:absolute;top:0;left:0;width:100%;height:100%;border:0;" 
                                            allowfullscreen></iframe>
                                </div>
                            </body>
                            </html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://videoplay.tncnursing.in", html, "text/html", "utf-8", null)
                    }
                },
                update = { webView ->
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <body style="margin:0;padding:0;background-color:black;">
                            <div style="position:relative;padding-bottom:56.25%;height:0;overflow:hidden;">
                                <iframe src="$videoUrlToLoad" 
                                        style="position:absolute;top:0;left:0;width:100%;height:100%;border:0;" 
                                        allowfullscreen></iframe>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL("https://videoplay.tncnursing.in", html, "text/html", "utf-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Playback Speed Controls
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = TncAmberSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Speed:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                listOf("0.5x", "1.0x", "1.25x", "1.5x", "2.0x").forEach { speed ->
                    FilterChip(
                        selected = selectedSpeed == speed,
                        onClick = { selectedSpeed = speed },
                        label = { Text(speed, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TncAmberSecondary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // Lecture Info & XP Claims Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = activeSession?.title ?: "Video Lecture",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TncNavyPrimary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Duration: ${activeSession?.duration ?: "45 mins"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TncNavyPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "XP",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+10 XP on Complete",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Key Lecture Highlights & Overview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeSession?.description?.ifBlank {
                            "Covers core clinical nursing interventions, high-yield exam points for NORCET & AIIMS, diagnostic parameters, and drug management protocols."
                        } ?: "Covers core clinical nursing interventions, high-yield exam points for NORCET & AIIMS, diagnostic parameters, and drug management protocols.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Complete Lecture & Claim XP Button
            Button(
                onClick = {
                    if (!isCompleted) {
                        isCompleted = true
                        viewModel.awardXp(10)
                        activeSession?.let {
                            viewModel.toggleBookmark(it.rowId, "video", it.title, "Completed Video")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) TncGreenSuccess else TncNavyPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                        contentDescription = "Complete",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompleted) "Lecture Marked Complete (+10 XP Earned!)" else "Mark Lecture as Complete (+10 XP)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
