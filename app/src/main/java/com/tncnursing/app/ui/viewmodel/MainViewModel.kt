package com.tncnursing.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tncnursing.app.data.local.AppDatabase
import com.tncnursing.app.data.local.BookmarkEntity
import com.tncnursing.app.data.local.QuizResultEntity
import com.tncnursing.app.data.local.RecentProgressEntity
import com.tncnursing.app.data.local.UserEntity
import com.tncnursing.app.data.models.Course
import com.tncnursing.app.data.models.LeaderboardUser
import com.tncnursing.app.data.models.Question
import com.tncnursing.app.data.models.Quiz
import com.tncnursing.app.data.models.Session
import com.tncnursing.app.data.models.SliderItem
import com.tncnursing.app.data.repository.TncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class QuizState(
    val quiz: Quiz? = null,
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, String> = emptyMap(), // questionIndex -> chosen option ("A", "B", "C", "D")
    val markedForReview: Set<Int> = emptySet(),
    val timeRemainingSeconds: Long = 0,
    val isSubmitted: Boolean = false,
    val score: Double = 0.0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val skippedCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TncRepository(db.appDao())

    val userProfile: StateFlow<UserEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentProgress: StateFlow<List<RecentProgressEntity>> = repository.recentProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizResultsHistory: StateFlow<List<QuizResultEntity>> = repository.quizResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Remote / Feed Data States
    private val _coursesState = MutableStateFlow<UiState<List<Course>>>(UiState.Loading)
    val coursesState: StateFlow<UiState<List<Course>>> = _coursesState.asStateFlow()

    private val _sessionsState = MutableStateFlow<UiState<List<Session>>>(UiState.Loading)
    val sessionsState: StateFlow<UiState<List<Session>>> = _sessionsState.asStateFlow()

    private val _quizzesState = MutableStateFlow<UiState<List<Quiz>>>(UiState.Loading)
    val quizzesState: StateFlow<UiState<List<Quiz>>> = _quizzesState.asStateFlow()

    private val _slidersState = MutableStateFlow<List<SliderItem>>(emptyList())
    val slidersState: StateFlow<List<SliderItem>> = _slidersState.asStateFlow()

    private val _leaderboardState = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboardState: StateFlow<List<LeaderboardUser>> = _leaderboardState.asStateFlow()

    // Active Selection States
    val selectedCourse = MutableStateFlow<Course?>(null)
    val selectedSession = MutableStateFlow<Session?>(null)

    // Active Quiz State
    val quizState = MutableStateFlow(QuizState())

    // Theme State
    val isDarkTheme = MutableStateFlow(false)

    // Downloaded E-Notes State (Set of session rowIds)
    private val _downloadedNotes = MutableStateFlow<Set<String>>(emptySet())
    val downloadedNotes: StateFlow<Set<String>> = _downloadedNotes.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                name = "Priya Sharma",
                targetExam = "NORCET 8.0 / AIIMS 2026",
                college = "AIIMS Nursing College",
                state = "New Delhi"
            )
        }
        loadHomeFeed()
        checkDownloadedNotes()
    }

    fun recordProgress(targetId: String, type: String, courseId: String, title: String, subtitle: String, progressSec: Long, totalSec: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.recordProgress(targetId, type, courseId, title, subtitle, progressSec, totalSec, completed)
        }
    }

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    private fun checkDownloadedNotes() {
        viewModelScope.launch {
            try {
                val dir = java.io.File(getApplication<Application>().filesDir, "enotes_downloads")
                if (dir.exists()) {
                    val downloadedFiles = dir.listFiles()?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()
                    _downloadedNotes.value = downloadedFiles
                }
            } catch (e: Exception) {
                // handle error gracefully
            }
        }
    }

    fun downloadNote(session: Session) {
        viewModelScope.launch {
            try {
                val dir = java.io.File(getApplication<Application>().filesDir, "enotes_downloads")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = java.io.File(dir, "${session.rowId}.pdf")
                file.writeText("PDF Content for ${session.title}\nURL: ${session.pdfUrl}")

                val updated = _downloadedNotes.value.toMutableSet()
                updated.add(session.rowId)
                _downloadedNotes.value = updated

                // Also record progress & award XP
                repository.recordProgress(
                    targetId = session.rowId,
                    type = "pdf",
                    courseId = session.courseId ?: "",
                    title = session.title,
                    subtitle = "Downloaded E-Note",
                    progressSec = 100,
                    totalSec = 100,
                    completed = true
                )
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            try {
                _coursesState.value = UiState.Loading
                _sessionsState.value = UiState.Loading
                _quizzesState.value = UiState.Loading

                val courses = repository.getCourses()
                _coursesState.value = UiState.Success(courses)

                val sessions = repository.getSessions()
                _sessionsState.value = UiState.Success(sessions)

                val quizzes = repository.getQuizzes()
                _quizzesState.value = UiState.Success(quizzes)

                _slidersState.value = repository.getSliders()
                _leaderboardState.value = repository.getLeaderboard()
            } catch (e: Exception) {
                e.printStackTrace()
                _coursesState.value = UiState.Error("Failed to load courses: ${e.localizedMessage}")
                _sessionsState.value = UiState.Error("Failed to load sessions: ${e.localizedMessage}")
                _quizzesState.value = UiState.Error("Failed to load quizzes: ${e.localizedMessage}")
            }
        }
    }

    fun loadCourseDetail(courseRowId: String) {
        viewModelScope.launch {
            try {
                val courses = (coursesState.value as? UiState.Success)?.data ?: repository.getCourses()
                val found = courses.find { it.rowId == courseRowId || it.id.toString() == courseRowId }
                selectedCourse.value = found

                _sessionsState.value = UiState.Loading
                val sessions = repository.getSessions(courseId = courseRowId)
                _sessionsState.value = UiState.Success(sessions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSessionDetail(sessionRowId: String) {
        viewModelScope.launch {
            try {
                val sessions = (sessionsState.value as? UiState.Success)?.data ?: repository.getSessions()
                val found = sessions.find { it.rowId == sessionRowId || it.id.toString() == sessionRowId }
                selectedSession.value = found
                found?.let {
                    repository.recordProgress(
                        targetId = it.rowId,
                        type = it.type,
                        courseId = it.courseId ?: "",
                        title = it.title,
                        subtitle = if (it.type == "pdf") "E-Note" else "Video Lecture",
                        progressSec = 10,
                        totalSec = 100,
                        completed = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startQuiz(examId: String) {
        viewModelScope.launch {
            try {
                val (quiz, questions) = repository.getQuizDetail(examId)
                val durationSec = (quiz.durationMinutes.toLongOrNull() ?: 30L) * 60L
                quizState.value = QuizState(
                    quiz = quiz,
                    questions = questions,
                    currentQuestionIndex = 0,
                    selectedAnswers = emptyMap(),
                    markedForReview = emptySet(),
                    timeRemainingSeconds = durationSec,
                    isSubmitted = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectQuizAnswer(questionIndex: Int, option: String) {
        val current = quizState.value
        val updated = current.selectedAnswers.toMutableMap()
        updated[questionIndex] = option
        quizState.value = current.copy(selectedAnswers = updated)
    }

    fun toggleMarkForReview(questionIndex: Int) {
        val current = quizState.value
        val updated = current.markedForReview.toMutableSet()
        if (updated.contains(questionIndex)) {
            updated.remove(questionIndex)
        } else {
            updated.add(questionIndex)
        }
        quizState.value = current.copy(markedForReview = updated)
    }

    fun goToQuizQuestion(index: Int) {
        val current = quizState.value
        if (index in current.questions.indices) {
            quizState.value = current.copy(currentQuestionIndex = index)
        }
    }

    fun submitQuiz() {
        val current = quizState.value
        val quiz = current.quiz ?: return
        var correct = 0
        var wrong = 0
        var skipped = 0

        current.questions.forEachIndexed { index, question ->
            val chosen = current.selectedAnswers[index]
            if (chosen == null) {
                skipped++
            } else if (chosen.equals(question.correctAnswer, ignoreCase = true) ||
                question.correctAnswer.startsWith(chosen, ignoreCase = true)) {
                correct++
            } else {
                wrong++
            }
        }

        val marksPerQuestion = quiz.maxMarks / (current.questions.size.coerceAtLeast(1))
        val rawScore = (correct * marksPerQuestion) - (wrong * quiz.negativeMarks * marksPerQuestion)
        val finalScore = rawScore.coerceAtLeast(0.0)

        quizState.value = current.copy(
            isSubmitted = true,
            score = finalScore,
            correctCount = correct,
            wrongCount = wrong,
            skippedCount = skipped
        )

        viewModelScope.launch {
            repository.recordQuizResult(
                QuizResultEntity(
                    examId = quiz.examId,
                    examName = quiz.name,
                    score = finalScore,
                    maxScore = quiz.maxMarks,
                    totalQuestions = current.questions.size,
                    correctCount = correct,
                    wrongCount = wrong,
                    skippedCount = skipped,
                    timeSpentSeconds = 300
                )
            )
        }
    }

    fun toggleBookmark(targetId: String, type: String, title: String, subtitle: String = "", imageUrl: String? = null) {
        viewModelScope.launch {
            val isCurrentlySaved = bookmarks.value.any { it.targetId == targetId }
            if (isCurrentlySaved) {
                repository.removeBookmark(targetId)
            } else {
                repository.toggleBookmark(targetId, type, title, subtitle, imageUrl)
            }
        }
    }

    fun updateUserProfile(name: String, targetExam: String, college: String, state: String) {
        viewModelScope.launch {
            repository.ensureUserProfile(name, targetExam, college, state)
        }
    }

    fun awardXp(points: Int) {
        viewModelScope.launch {
            repository.awardXp(points)
        }
    }
}
