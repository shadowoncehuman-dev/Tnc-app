package com.tncnursing.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Nursing Student",
    val mobile: String = "",
    val targetExam: String = "NORCET 2026",
    val college: String = "",
    val state: String = "",
    val totalXp: Int = 25,
    val currentStreak: Int = 1,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetId: String,
    val type: String, // "course", "session", "pdf", "quiz"
    val title: String,
    val subtitle: String = "",
    val imageUrl: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_progress")
data class RecentProgressEntity(
    @PrimaryKey val targetId: String, // rowId of session or exam
    val type: String, // "video", "pdf", "quiz"
    val courseId: String = "",
    val title: String,
    val subtitle: String = "",
    val progressSeconds: Long = 0,
    val totalSeconds: Long = 0,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: String,
    val examName: String,
    val score: Double,
    val maxScore: Double,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val timeSpentSeconds: Long,
    val completedAt: Long = System.currentTimeMillis()
)
