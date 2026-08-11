package com.tncnursing.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileSync(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(user: UserEntity)

    @Query("UPDATE user_profile SET totalXp = totalXp + :additionalXp WHERE id = 1")
    suspend fun addXp(additionalXp: Int)

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE targetId = :targetId)")
    fun isBookmarked(targetId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE targetId = :targetId")
    suspend fun removeBookmarkByTargetId(targetId: String)

    // Recent Progress
    @Query("SELECT * FROM recent_progress ORDER BY updatedAt DESC LIMIT 20")
    fun getRecentProgress(): Flow<List<RecentProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecentProgress(progress: RecentProgressEntity)

    @Query("SELECT * FROM recent_progress WHERE targetId = :targetId LIMIT 1")
    suspend fun getProgressForTarget(targetId: String): RecentProgressEntity?

    // Quiz Results
    @Query("SELECT * FROM quiz_results ORDER BY completedAt DESC")
    fun getQuizResults(): Flow<List<QuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity)
}
