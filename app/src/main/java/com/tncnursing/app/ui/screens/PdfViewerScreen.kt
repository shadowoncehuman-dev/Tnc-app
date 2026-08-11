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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tncnursing.app.ui.theme.TncAmberSecondary
import com.tncnursing.app.ui.theme.TncGreenSuccess
import com.tncnursing.app.ui.theme.TncNavyPrimary
import com.tncnursing.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    sessionRowId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(sessionRowId) {
        viewModel.loadSessionDetail(sessionRowId)
    }

    val session by viewModel.selectedSession.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val downloadedNotes by viewModel.downloadedNotes.collectAsState()
    var isReadCompleted by remember { mutableStateOf(false) }

    val activeSession = session
    val isDownloaded = downloadedNotes.contains(sessionRowId)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = activeSession?.title ?: "PDF E-Note",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = {
                    activeSession?.let { viewModel.downloadNote(it) }
                }) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                        contentDescription = "Download E-Note",
                        tint = if (isDownloaded) TncAmberSecondary else Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TncNavyPrimary)
        )

        // Security / Watermark Header Banner
        Surface(
            color = Color(0xFFFEF3C7),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Protected",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Watermarked for ${userProfile?.name ?: "Student"} • Copyright TNC Nursing Classes",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF92400E)
                )
            }
        }

        // PDF Viewport Canvas with Watermark Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewClient = WebViewClient()
                        // Embedded Google Docs PDF viewer or inline document
                        val targetUrl = "https://docs.google.com/viewer?url=https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/examples/learning/helloworld.pdf&embedded=true"
                        loadUrl(targetUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Transparent Diagonal Watermark Overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.08f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PROPERTY OF ${userProfile?.name?.uppercase() ?: "STUDENT"}\nTNC NURSING CLASSES",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }

        // Bottom Action Bar
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Read Time: >30 Secs",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Reward: +5 XP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TncNavyPrimary
                    )
                }

                Button(
                    onClick = {
                        if (!isReadCompleted) {
                            isReadCompleted = true
                            viewModel.awardXp(5)
                            activeSession?.let {
                                viewModel.toggleBookmark(it.rowId, "pdf", it.title, "Completed E-Note")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReadCompleted) TncGreenSuccess else TncNavyPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isReadCompleted) Icons.Default.CheckCircle else Icons.Default.Description,
                            contentDescription = "Read Note",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isReadCompleted) "E-Note Finished (+5 XP)" else "Mark Finished (+5 XP)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
