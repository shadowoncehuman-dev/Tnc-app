package com.tncnursing.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tncnursing.app.ui.theme.TncAmberSecondary
import com.tncnursing.app.ui.theme.TncNavyPrimary
import com.tncnursing.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTakeScreen(
    examId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onQuizSubmitted: (String) -> Unit
) {
    LaunchedEffect(examId) {
        viewModel.startQuiz(examId)
    }

    val state by viewModel.quizState.collectAsState()
    var showSubmitDialog by remember { mutableStateOf(false) }

    val quiz = state.quiz
    val questions = state.questions

    if (quiz == null || questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TncNavyPrimary)
        }
        return
    }

    val currentIdx = state.currentQuestionIndex
    val currentQuestion = questions.getOrNull(currentIdx)

    if (currentQuestion == null) return

    val selectedOption = state.selectedAnswers[currentIdx]
    val isMarked = state.markedForReview.contains(currentIdx)

    Column(modifier = Modifier.fillMaxSize()) {
        // App Bar with Timer
        TopAppBar(
            title = {
                Text(
                    text = quiz.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Exit", tint = Color.White)
                }
            },
            actions = {
                // Live Timer Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Timer",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${quiz.durationMinutes}:00",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TncNavyPrimary)
        )

        // Question Navigator Row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(questions) { idx, q ->
                    val isCurrent = idx == currentIdx
                    val isAnswered = state.selectedAnswers.containsKey(idx)
                    val isReview = state.markedForReview.contains(idx)

                    val bgColor = when {
                        isCurrent -> TncNavyPrimary
                        isReview -> Color(0xFFFEF3C7)
                        isAnswered -> Color(0xFFD1FAE5)
                        else -> Color(0xFFF1F5F9)
                    }

                    val textColor = when {
                        isCurrent -> Color.White
                        isReview -> Color(0xFF92400E)
                        isAnswered -> Color(0xFF065F46)
                        else -> Color.DarkGray
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(bgColor, shape = CircleShape)
                            .clickable { viewModel.goToQuizQuestion(idx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Question Body
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${currentIdx + 1} of ${questions.size}",
                    fontWeight = FontWeight.Bold,
                    color = TncNavyPrimary,
                    fontSize = 14.sp
                )

                IconButton(onClick = { viewModel.toggleMarkForReview(currentIdx) }) {
                    Icon(
                        imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Mark for review",
                        tint = if (isMarked) TncAmberSecondary else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentQuestion.questionText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Options List
            val options = listOf(
                "A" to currentQuestion.optionA,
                "B" to currentQuestion.optionB,
                "C" to currentQuestion.optionC,
                "D" to currentQuestion.optionD
            )

            options.forEach { (label, text) ->
                if (text.isNotBlank()) {
                    val isSelected = selectedOption == label
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { viewModel.selectQuizAnswer(currentIdx, label) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) TncNavyPrimary else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectQuizAnswer(currentIdx, label) },
                                colors = RadioButtonDefaults.colors(selectedColor = TncNavyPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "($label) $text",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
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
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.goToQuizQuestion(currentIdx - 1) },
                    enabled = currentIdx > 0
                ) {
                    Text("Previous")
                }

                Button(
                    onClick = {
                        if (currentIdx < questions.size - 1) {
                            viewModel.goToQuizQuestion(currentIdx + 1)
                        } else {
                            showSubmitDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TncNavyPrimary)
                ) {
                    Text(if (currentIdx == questions.size - 1) "Finish Test" else "Next Question")
                }
            }
        }
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Exam?") },
            text = {
                Text("Answered: ${state.selectedAnswers.size} of ${questions.size} questions.\nMarked for review: ${state.markedForReview.size}.\nAre you sure you want to finalize your submission?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        viewModel.submitQuiz()
                        onQuizSubmitted(examId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TncNavyPrimary)
                ) {
                    Text("Confirm Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Keep Testing")
                }
            }
        )
    }
}
