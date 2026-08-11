package com.tncnursing.app.network

import com.google.gson.JsonElement
import com.tncnursing.app.data.models.Course
import com.tncnursing.app.data.models.Quiz
import com.tncnursing.app.data.models.Session
import com.tncnursing.app.data.models.SliderItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class CrmPayloadRequest(
    val payload: String
)

data class QuizListResponse(
    val quizzes: List<Quiz> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

data class QuizDetailResponse(
    val examId: String = "",
    val examNo: Int = 0,
    val name: String = "",
    val maxMarks: Double = 100.0,
    val negativeMarks: Double = 0.25,
    val durationMinutes: String = "60",
    val questionCount: Int = 0,
    val validUntil: String? = null,
    val category: String = "NORCET / AIIMS",
    val questions: List<com.tncnursing.app.data.models.Question> = emptyList()
)

interface TncApiService {

    @POST("common/")
    suspend fun postCrmCommon(
        @Body request: CrmPayloadRequest
    ): Response<JsonElement>

    @GET("/api/courses")
    suspend fun getCourses(): Response<List<Course>>

    @GET("/api/sessions")
    suspend fun getSessions(
        @Query("courseId") courseId: String? = null,
        @Query("type") type: String? = null,
        @Query("sort") sort: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Session>>

    @GET("/api/sessions/{rowId}")
    suspend fun getSessionDetail(
        @Path("rowId") rowId: String
    ): Response<Session>

    @GET("/api/notes")
    suspend fun getNotes(
        @Query("courseId") courseId: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Session>>

    @GET("/api/quizzes")
    suspend fun getQuizzes(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<QuizListResponse>

    @GET("/api/quiz/{examId}")
    suspend fun getQuizDetail(
        @Path("examId") examId: String
    ): Response<QuizDetailResponse>

    @GET("/api/sliders")
    suspend fun getSliders(): Response<List<SliderItem>>
}
