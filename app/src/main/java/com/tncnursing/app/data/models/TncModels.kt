package com.tncnursing.app.data.models

data class Course(
    val id: Long = 0,
    val rowId: String = "",
    val name: String = "",
    val description: String = "",
    val serialNo: String = "",
    val imageUrl: String? = null,
    val videoCount: Int = 0,
    val pdfCount: Int = 0,
    val isPaid: Boolean = false,
    val category: String = "General Nursing",
    val createdAt: String? = null
)

data class Session(
    val id: Long = 0,
    val rowId: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String? = null,
    val pdfUrl: String? = null,
    val firebaseId: String? = null,
    val contentType: String = "none", // youtube, firebase, pdf, none
    val type: String = "content", // video, pdf, content
    val courseId: String? = null,
    val subjectId: String? = null,
    val isPaid: Boolean = false,
    val serialNo: String = "",
    val duration: String? = null,
    val createdAt: String? = null
)

data class Quiz(
    val examId: String = "",
    val examNo: Int = 0,
    val name: String = "",
    val maxMarks: Double = 100.0,
    val negativeMarks: Double = 0.25,
    val durationMinutes: String = "60",
    val questionCount: Int = 0,
    val validUntil: String? = null,
    val category: String = "NORCET / AIIMS"
)

data class Question(
    val rowId: String = "",
    val questionId: Long = 0,
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctAnswer: String = "", // e.g. "A", "B", "C", "D" or full text
    val explanation: String? = null,
    val questionNo: Int? = null
)

data class SliderItem(
    val id: Long = 0,
    val rowId: String = "",
    val imageUrl: String = "",
    val name: String = "",
    val description: String = ""
)

data class LeaderboardUser(
    val rank: Int,
    val name: String,
    val xp: Int,
    val streakDays: Int,
    val isCurrentUser: Boolean = false
)
