package com.tncnursing.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Courses : Screen("courses")
    object CourseDetail : Screen("course_detail/{courseRowId}") {
        fun createRoute(courseRowId: String) = "course_detail/$courseRowId"
    }
    object Videos : Screen("videos")
    object VideoPlayer : Screen("video_player/{sessionRowId}") {
        fun createRoute(sessionRowId: String) = "video_player/$sessionRowId"
    }
    object Enotes : Screen("enotes")
    object PdfViewer : Screen("pdf_viewer/{sessionRowId}") {
        fun createRoute(sessionRowId: String) = "pdf_viewer/$sessionRowId"
    }
    object QuizList : Screen("quiz_list")
    object QuizTake : Screen("quiz_take/{examId}") {
        fun createRoute(examId: String) = "quiz_take/$examId"
    }
    object QuizResult : Screen("quiz_result/{examId}") {
        fun createRoute(examId: String) = "quiz_result/$examId"
    }
    object Leaderboard : Screen("leaderboard")
    object Bookmarks : Screen("bookmarks")
    object Profile : Screen("profile")
}
