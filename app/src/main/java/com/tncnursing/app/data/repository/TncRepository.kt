package com.tncnursing.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.tncnursing.app.data.local.AppDao
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
import com.tncnursing.app.network.CrmPayloadRequest
import com.tncnursing.app.network.TncApiService
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TncRepository(private val appDao: AppDao) {

    private val apiService: TncApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://crm.tncnursing.in/") // Base fallback endpoint
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TncApiService::class.java)
    }

    // --- User Profile & XP ---
    val userProfile: Flow<UserEntity?> = appDao.getUserProfile()

    suspend fun ensureUserProfile(name: String, targetExam: String, college: String = "", state: String = "") {
        val existing = appDao.getUserProfileSync()
        if (existing == null) {
            appDao.saveUserProfile(
                UserEntity(
                    id = 1,
                    name = name,
                    targetExam = targetExam,
                    college = college,
                    state = state,
                    totalXp = 50,
                    currentStreak = 1
                )
            )
        } else {
            appDao.saveUserProfile(
                existing.copy(
                    name = name.ifBlank { existing.name },
                    targetExam = targetExam.ifBlank { existing.targetExam },
                    college = college.ifBlank { existing.college },
                    state = state.ifBlank { existing.state }
                )
            )
        }
    }

    suspend fun awardXp(points: Int) {
        appDao.addXp(points)
    }

    // --- Bookmarks & Recent Progress ---
    val bookmarks: Flow<List<BookmarkEntity>> = appDao.getAllBookmarks()
    val recentProgress: Flow<List<RecentProgressEntity>> = appDao.getRecentProgress()
    val quizResults: Flow<List<QuizResultEntity>> = appDao.getQuizResults()

    fun isBookmarked(targetId: String): Flow<Boolean> = appDao.isBookmarked(targetId)

    suspend fun toggleBookmark(targetId: String, type: String, title: String, subtitle: String = "", imageUrl: String? = null) {
        val bookmark = BookmarkEntity(
            targetId = targetId,
            type = type,
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl
        )
        appDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(targetId: String) {
        appDao.removeBookmarkByTargetId(targetId)
    }

    suspend fun recordProgress(targetId: String, type: String, courseId: String, title: String, subtitle: String, progressSec: Long, totalSec: Long, completed: Boolean) {
        val entity = RecentProgressEntity(
            targetId = targetId,
            type = type,
            courseId = courseId,
            title = title,
            subtitle = subtitle,
            progressSeconds = progressSec,
            totalSeconds = totalSec,
            isCompleted = completed,
            updatedAt = System.currentTimeMillis()
        )
        appDao.saveRecentProgress(entity)
        if (completed) {
            val xpToAward = when (type) {
                "video" -> 10
                "pdf" -> 5
                "quiz" -> 20
                else -> 5
            }
            awardXp(xpToAward)
        }
    }

    suspend fun recordQuizResult(result: QuizResultEntity) {
        appDao.insertQuizResult(result)
        // Award XP on quiz completion (+20 XP minimum)
        val earnedXp = (20 + (result.score * 0.5)).toInt().coerceAtLeast(20)
        awardXp(earnedXp)
    }

    // --- Remote Live CRM API Integrations ---
    private fun JsonElement?.getStr(): String {
        if (this == null || this.isJsonNull) return ""
        return try {
            if (this.isJsonPrimitive) this.asString else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun JsonObject?.optString(key: String, fallback: String = ""): String {
        if (this == null || !has(key)) return fallback
        val elem = get(key)
        val str = elem.getStr()
        return if (str.isNotEmpty()) str else fallback
    }

    private fun JsonObject?.optInt(key: String, fallback: Int = 0): Int {
        if (this == null || !has(key)) return fallback
        val elem = get(key)
        if (elem == null || elem.isJsonNull) return fallback
        return try {
            if (elem.isJsonPrimitive) elem.asInt else fallback
        } catch (e: Exception) {
            try { elem.asString.toInt() } catch (e2: Exception) { fallback }
        }
    }

    private fun JsonObject?.optDouble(key: String, fallback: Double = 0.0): Double {
        if (this == null || !has(key)) return fallback
        val elem = get(key)
        if (elem == null || elem.isJsonNull) return fallback
        return try {
            if (elem.isJsonPrimitive) elem.asDouble else fallback
        } catch (e: Exception) {
            try { elem.asString.toDouble() } catch (e2: Exception) { fallback }
        }
    }

    private fun fixMediaUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("uploads/") -> "https://crm.tncnursing.in/$url"
            url.startsWith("/uploads/") -> "https://crm.tncnursing.in$url"
            else -> "https://crm.tncnursing.in/uploads/$url"
        }
    }

    private fun buildPayload(table: String, condMap: Map<String, Any> = emptyMap()): String {
        val map = mapOf(
            "fn" to "common_fn",
            "se" to "fe",
            "sch" to table,
            "data" to mapOf("json" to "*"),
            "cond" to condMap
        )
        return Gson().toJson(map)
    }

    private suspend fun fetchCrmArray(table: String, condMap: Map<String, Any> = emptyMap()): JsonArray? {
        try {
            val payloadStr = buildPayload(table, condMap)
            val resp = apiService.postCrmCommon(CrmPayloadRequest(payloadStr))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.isJsonArray) {
                    return body.asJsonArray
                } else if (body.isJsonObject && body.asJsonObject.has("data") && body.asJsonObject.get("data").isJsonArray) {
                    return body.asJsonObject.getAsJsonArray("data")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun getCourses(): List<Course> {
        try {
            val jsonArray = fetchCrmArray("t_co")
            if (jsonArray != null && jsonArray.size() > 0) {
                val courses = mutableListOf<Course>()
                jsonArray.forEachIndexed { index, elem ->
                    try {
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val rowId = obj.optString("row_id", "co_$index")
                            val json = if (obj.has("json") && obj.get("json").isJsonObject) obj.getAsJsonObject("json") else null
                            val name = json.optString("_na", "Course ${index + 1}")
                            val desc = json.optString("_de", "")
                            val sno = json.optString("_sno", "${index + 1}")
                            val isPaid = json.optInt("_pr_ty", 0) == 1

                            var imgUrl = ""
                            if (json != null && json.has("_at")) {
                                val at = json.get("_at")
                                if (at != null && at.isJsonObject) {
                                    imgUrl = at.asJsonObject.optString("url", "")
                                } else if (at != null && at.isJsonPrimitive) {
                                    imgUrl = at.getStr()
                                }
                            }
                            imgUrl = fixMediaUrl(imgUrl)
                            if (imgUrl.isBlank()) {
                                imgUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800"
                            }

                            courses.add(
                                Course(
                                    id = (index + 1).toLong(),
                                    rowId = rowId,
                                    name = name,
                                    description = desc,
                                    serialNo = sno,
                                    imageUrl = imgUrl,
                                    videoCount = 24,
                                    pdfCount = 12,
                                    isPaid = isPaid,
                                    category = if (name.contains("NORCET", ignoreCase = true)) "NORCET" else "Special Batch"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (courses.isNotEmpty()) {
                    return courses.sortedByDescending { it.id }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getFallbackCourses()
    }

    suspend fun getSessions(courseId: String? = null, type: String? = null): List<Session> {
        try {
            val cond = if (!courseId.isNullOrBlank()) mapOf("co_refid" to courseId) else emptyMap<String, Any>()
            val jsonArray = fetchCrmArray("t_ch", cond)
            if (jsonArray != null && jsonArray.size() > 0) {
                val sessions = mutableListOf<Session>()
                jsonArray.forEachIndexed { index, elem ->
                    try {
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val rowId = obj.optString("row_id", "ch_$index")
                            val cRef = obj.optString("co_refid", courseId ?: "")
                            val json = if (obj.has("json") && obj.get("json").isJsonObject) obj.getAsJsonObject("json") else null
                            val title = json.optString("_na", "Lecture ${index + 1}")
                            val desc = json.optString("_de", "")
                            val sno = json.optString("_sno", "${index + 1}")

                            var viUrl = ""
                            var fsId = ""
                            if (json != null && json.has("_vi") && json.get("_vi").isJsonObject) {
                                val viObj = json.getAsJsonObject("_vi")
                                viUrl = viObj.optString("_vi_url", "")
                                fsId = viObj.optString("_fs_id", "")
                            }

                            var pdfUrl = ""
                            if (json != null && json.has("_pdf")) {
                                pdfUrl = fixMediaUrl(json.get("_pdf").getStr())
                            } else if (json != null && json.has("_at")) {
                                val at = json.get("_at")
                                if (at != null && at.isJsonObject) {
                                    pdfUrl = fixMediaUrl(at.asJsonObject.optString("url", ""))
                                }
                            }

                            val contentType = when {
                                fsId.isNotBlank() -> "fs"
                                viUrl.isNotBlank() -> "youtube"
                                pdfUrl.isNotBlank() -> "pdf"
                                else -> "youtube"
                            }

                            val finalVideoUrl = when {
                                fsId.isNotBlank() -> "https://videoplay.tncnursing.in/videos/fs/index.html?$fsId"
                                viUrl.isNotBlank() -> viUrl
                                else -> ""
                            }

                            val sessType = if (contentType == "pdf" || (viUrl.isBlank() && fsId.isBlank() && pdfUrl.isNotBlank())) "pdf" else "video"

                            if (type == null || sessType == type) {
                                sessions.add(
                                    Session(
                                        id = (index + 100).toLong(),
                                        rowId = rowId,
                                        title = title,
                                        description = desc,
                                        videoUrl = finalVideoUrl.ifBlank { "https://www.youtube.com/watch?v=dQw4w9WgXcQ" },
                                        pdfUrl = if (pdfUrl.isNotBlank()) pdfUrl else null,
                                        contentType = contentType,
                                        type = sessType,
                                        courseId = cRef,
                                        serialNo = sno,
                                        duration = if (sessType == "pdf") "12 pages" else "45 mins"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (sessions.isNotEmpty()) {
                    return sessions.sortedByDescending { it.id }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getFallbackSessions(courseId)
    }

    suspend fun getNotes(courseId: String? = null): List<Session> {
        return getSessions(courseId, "pdf")
    }

    suspend fun getQuizzes(query: String? = null): List<Quiz> {
        try {
            val jsonArray = fetchCrmArray("t_ex")
            if (jsonArray != null && jsonArray.size() > 0) {
                val quizzes = mutableListOf<Quiz>()
                jsonArray.forEachIndexed { index, elem ->
                    try {
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val rowId = obj.optString("row_id", "ex_$index")
                            val examNo = obj.optInt("examno", index + 800)
                            val json = if (obj.has("json") && obj.get("json").isJsonObject) obj.getAsJsonObject("json") else null
                            val name = json.optString("_ex_na", "Exam ${index + 1}")
                            val maxMarks = json.optDouble("_ma_ma", 100.0)
                            val negMarks = json.optDouble("_ne_ma", 0.25)
                            val duration = json.optString("_ex_du", "60")

                            var quCount = 10
                            if (obj.has("qu_refid") && obj.get("qu_refid").isJsonArray) {
                                quCount = obj.getAsJsonArray("qu_refid").size()
                            }

                            if (query.isNullOrBlank() || name.contains(query, ignoreCase = true)) {
                                quizzes.add(
                                    Quiz(
                                        examId = rowId,
                                        examNo = examNo,
                                        name = name,
                                        maxMarks = maxMarks,
                                        negativeMarks = negMarks,
                                        durationMinutes = duration,
                                        questionCount = quCount,
                                        category = "NORCET / AIIMS Exam"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (quizzes.isNotEmpty()) {
                    return quizzes
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getFallbackQuizzes(query)
    }

    suspend fun getQuizDetail(examId: String): Pair<Quiz, List<Question>> {
        try {
            val jsonArray = fetchCrmArray("t_ex", mapOf("row_id" to examId))
            if (jsonArray != null && jsonArray.size() > 0 && jsonArray.get(0).isJsonObject) {
                val obj = jsonArray.get(0).asJsonObject
                val rowId = obj.optString("row_id", examId)
                val examNo = obj.optInt("examno", 801)
                val json = if (obj.has("json") && obj.get("json").isJsonObject) obj.getAsJsonObject("json") else null
                val name = json.optString("_ex_na", "TNC Practice Exam")
                val maxMarks = json.optDouble("_ma_ma", 100.0)
                val negMarks = json.optDouble("_ne_ma", 0.25)
                val duration = json.optString("_ex_du", "60")

                val questionList = mutableListOf<Question>()

                if (obj.has("qu_refid") && obj.get("qu_refid").isJsonArray) {
                    val qRefs = obj.getAsJsonArray("qu_refid")
                    val maxToFetch = minOf(qRefs.size(), 15)
                    for (i in 0 until maxToFetch) {
                        try {
                            val qRowId = qRefs.get(i).getStr()
                            if (qRowId.isNotBlank()) {
                                val qArray = fetchCrmArray("t_qu", mapOf("row_id" to qRowId))
                                if (qArray != null && qArray.size() > 0 && qArray.get(0).isJsonObject) {
                                    val qObj = qArray.get(0).asJsonObject
                                    val qJson = if (qObj.has("json") && qObj.get("json").isJsonObject) qObj.getAsJsonObject("json") else null
                                    val qText = qJson?.getAsJsonObject("_qu")?.optString("_qu", "Question ${i + 1}") ?: "Question ${i + 1}"

                                    val opObj = qJson?.getAsJsonObject("_op")
                                    val opA = opObj?.getAsJsonObject("_op_A")?.optString("_op_ti", "Option A") ?: "Option A"
                                    val opB = opObj?.getAsJsonObject("_op_B")?.optString("_op_ti", "Option B") ?: "Option B"
                                    val opC = opObj?.getAsJsonObject("_op_C")?.optString("_op_ti", "Option C") ?: "Option C"
                                    val opD = opObj?.getAsJsonObject("_op_D")?.optString("_op_ti", "Option D") ?: "Option D"

                                    val ans = qJson.optString("_an", "A")
                                    val sol = qJson?.getAsJsonObject("_so")?.optString("_ti", "Detailed solution provided.") ?: "Detailed solution provided."

                                    questionList.add(
                                        Question(
                                            rowId = qRowId,
                                            questionId = (i + 1).toLong(),
                                            questionText = qText,
                                            optionA = opA,
                                            optionB = opB,
                                            optionC = opC,
                                            optionD = opD,
                                            correctAnswer = ans,
                                            explanation = sol,
                                            questionNo = i + 1
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (questionList.isNotEmpty()) {
                    val quiz = Quiz(
                        examId = rowId,
                        examNo = examNo,
                        name = name,
                        maxMarks = maxMarks,
                        negativeMarks = negMarks,
                        durationMinutes = duration,
                        questionCount = questionList.size
                    )
                    return Pair(quiz, questionList)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getFallbackQuizDetail(examId)
    }

    suspend fun getSliders(): List<SliderItem> {
        try {
            val jsonArray = fetchCrmArray("t_sl")
            if (jsonArray != null && jsonArray.size() > 0) {
                val sliders = mutableListOf<SliderItem>()
                jsonArray.forEachIndexed { index, elem ->
                    try {
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val rowId = obj.optString("row_id", "sl_$index")
                            val json = if (obj.has("json") && obj.get("json").isJsonObject) obj.getAsJsonObject("json") else null
                            val name = json.optString("_na", "TNC Announcement")
                            val desc = json.optString("_de", "")

                            var imgUrl = ""
                            if (json != null && json.has("_at")) {
                                val at = json.get("_at")
                                if (at != null && at.isJsonObject) {
                                    imgUrl = at.asJsonObject.optString("url", "")
                                } else if (at != null && at.isJsonPrimitive) {
                                    imgUrl = at.getStr()
                                }
                            }
                            imgUrl = fixMediaUrl(imgUrl)

                            if (imgUrl.isNotBlank()) {
                                sliders.add(
                                    SliderItem(
                                        id = (index + 1).toLong(),
                                        rowId = rowId,
                                        imageUrl = imgUrl,
                                        name = name,
                                        description = desc
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (sliders.isNotEmpty()) {
                    return sliders
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getFallbackSliders()
    }

    fun getLeaderboard(): List<LeaderboardUser> {
        return listOf(
            LeaderboardUser(1, "Aakash Sharma", 1450, 24),
            LeaderboardUser(2, "Priya Singh", 1320, 19),
            LeaderboardUser(3, "Ananya Verma", 1280, 21),
            LeaderboardUser(4, "Rahul Meena", 1190, 15),
            LeaderboardUser(5, "Sneha Patel", 1120, 14),
            LeaderboardUser(6, "Vikas Gupta", 1050, 12),
            LeaderboardUser(7, "Pooja Chaudhary", 980, 11),
            LeaderboardUser(8, "Sunil Kumar", 940, 9),
            LeaderboardUser(9, "Kavita Yadav", 890, 8),
            LeaderboardUser(10, "Deepak Joshi", 850, 7)
        )
    }

    // --- High-Quality Fallback Data Generators ---
    private fun getFallbackCourses(): List<Course> {
        return listOf(
            Course(
                id = 1,
                rowId = "c_norcet_2026",
                name = "NORCET 8.0 / AIIMS Target Batch 2026",
                description = "Comprehensive master class covering Medical Surgical Nursing, Anatomy, Pharmacology, and Community Health for NORCET preparation.",
                serialNo = "01",
                imageUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800",
                videoCount = 48,
                pdfCount = 32,
                isPaid = false,
                category = "NORCET"
            ),
            Course(
                id = 2,
                rowId = "c_esic_rr_2026",
                name = "ESIC & RRB Staff Nurse Special Batch",
                description = "Focused crash course with high-yield MCQs, image-based questions, and clinical nursing management for ESIC and Railway exams.",
                serialNo = "02",
                imageUrl = "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=800",
                videoCount = 36,
                pdfCount = 24,
                isPaid = false,
                category = "ESIC / RRB"
            ),
            Course(
                id = 3,
                rowId = "c_anatomy_physio",
                name = "Anatomy & Physiology Core Module",
                description = "Detailed system-wise breakdown of Cardiovascular, Respiratory, Nervous, and Renal systems with 3D diagrams and clinical correlates.",
                serialNo = "03",
                imageUrl = "https://images.unsplash.com/photo-1532938911079-1b06ac7ceec7?w=800",
                videoCount = 25,
                pdfCount = 18,
                isPaid = false,
                category = "Core Subjects"
            ),
            Course(
                id = 4,
                rowId = "c_pharma_nursing",
                name = "Pharmacology & Dosage Calculations Masterclass",
                description = "Drug classifications, mechanism of action, antidote cheat sheets, IV fluid rates, and paediatric drug dosage calculation formulas.",
                serialNo = "04",
                imageUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=800",
                videoCount = 20,
                pdfCount = 15,
                isPaid = false,
                category = "Core Subjects"
            ),
            Course(
                id = 5,
                rowId = "c_pediatric_obg",
                name = "Pediatric & OBG Nursing Special",
                description = "Maternal-child health, APGAR scoring, developmental milestones, high-risk pregnancy management, and newborn resuscitation guidelines.",
                serialNo = "05",
                imageUrl = "https://images.unsplash.com/photo-1516549655169-df83a0774514?w=800",
                videoCount = 30,
                pdfCount = 22,
                isPaid = false,
                category = "Specialty"
            )
        )
    }

    private fun getFallbackSessions(courseId: String?): List<Session> {
        val all = listOf(
            Session(
                id = 101,
                rowId = "s_norcet_01",
                title = "Cardiovascular System — Anatomy & Physiology of Heart",
                description = "Conduction system, cardiac cycle, coronary circulation, and ECG waveform interpretation for nursing exams.",
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                contentType = "youtube",
                type = "video",
                courseId = "c_norcet_2026",
                serialNo = "1.01",
                duration = "45 mins"
            ),
            Session(
                id = 102,
                rowId = "s_norcet_02_pdf",
                title = "ECG Interpretation & Arrhythmia E-Note",
                description = "Comprehensive reference guide with strip samples for VT, VF, Atrial Fibrillation, and STEMI identification.",
                pdfUrl = "https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/examples/learning/helloworld.pdf",
                contentType = "pdf",
                type = "pdf",
                courseId = "c_norcet_2026",
                serialNo = "1.02",
                duration = "15 pages"
            ),
            Session(
                id = 103,
                rowId = "s_norcet_03",
                title = "Myocardial Infarction & Acute Coronary Syndrome",
                description = "Pathophysiology, MONA protocol, thrombolytic therapy, and post-CABG nursing care responsibilities.",
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                contentType = "youtube",
                type = "video",
                courseId = "c_norcet_2026",
                serialNo = "1.03",
                duration = "52 mins"
            ),
            Session(
                id = 201,
                rowId = "s_esic_01",
                title = "Common Infection Control Protocols & Bio-Medical Waste Management",
                description = "BMW color coding rules (Yellow, Red, White, Blue), PPE donning/doffing sequence, and hand hygiene steps.",
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                contentType = "youtube",
                type = "video",
                courseId = "c_esic_rr_2026",
                serialNo = "2.01",
                duration = "38 mins"
            ),
            Session(
                id = 202,
                rowId = "s_esic_02_pdf",
                title = "Biomedical Waste & Isolation Precautions Chart PDF",
                description = "Printable quick revision cheat sheet for ESIC and AIIMS NORCET image-based questions.",
                pdfUrl = "https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/examples/learning/helloworld.pdf",
                contentType = "pdf",
                type = "pdf",
                courseId = "c_esic_rr_2026",
                serialNo = "2.02",
                duration = "10 pages"
            ),
            Session(
                id = 301,
                rowId = "s_pharma_01",
                title = "Cardiac Drugs — Digoxin, Nitroglycerin & Antihypertensives",
                description = "Mechanism, dosage, apical pulse monitoring before Digoxin, signs of toxicity, and patient education.",
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                contentType = "youtube",
                type = "video",
                courseId = "c_pharma_nursing",
                serialNo = "3.01",
                duration = "40 mins"
            ),
            Session(
                id = 302,
                rowId = "s_pharma_02_pdf",
                title = "Emergency Antidotes & Drug Dosage Calculation Formulae",
                description = "Complete formula sheet for drop rates (gtt/min), paediatric dosage rules (Young's, Clark's), and antidote chart.",
                pdfUrl = "https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/examples/learning/helloworld.pdf",
                contentType = "pdf",
                type = "pdf",
                courseId = "c_pharma_nursing",
                serialNo = "3.02",
                duration = "12 pages"
            )
        )

        return if (!courseId.isNullOrBlank()) {
            all.filter { it.courseId == courseId }
        } else {
            all
        }
    }

    private fun getFallbackQuizzes(query: String?): List<Quiz> {
        val list = listOf(
            Quiz(
                examId = "q_norcet_grand_01",
                examNo = 801,
                name = "NORCET 8.0 Grand Mock Test — All Subjects",
                maxMarks = 200.0,
                negativeMarks = 0.33,
                durationMinutes = "180",
                questionCount = 10,
                category = "NORCET Grand Test"
            ),
            Quiz(
                examId = "q_msn_cardio_01",
                examNo = 802,
                name = "Medical Surgical Nursing — Cardiovascular Special Test",
                maxMarks = 50.0,
                negativeMarks = 0.25,
                durationMinutes = "45",
                questionCount = 10,
                category = "Subject Test"
            ),
            Quiz(
                examId = "q_bmw_infection_01",
                examNo = 803,
                name = "Biomedical Waste & Infection Control Rapid Quiz",
                maxMarks = 25.0,
                negativeMarks = 0.25,
                durationMinutes = "20",
                questionCount = 8,
                category = "Topic Rapid Quiz"
            ),
            Quiz(
                examId = "q_pharma_dosage_01",
                examNo = 804,
                name = "Pharmacology & IV Calculations Practice Test",
                maxMarks = 30.0,
                negativeMarks = 0.25,
                durationMinutes = "30",
                questionCount = 8,
                category = "Topic Rapid Quiz"
            )
        )

        return if (!query.isNullOrBlank()) {
            list.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            list
        }
    }

    private fun getFallbackQuizDetail(examId: String): Pair<Quiz, List<Question>> {
        val quiz = Quiz(
            examId = examId,
            examNo = 801,
            name = if (examId.contains("cardio")) "Cardiovascular System High-Yield Test" else "NORCET Master Practice Quiz",
            maxMarks = 10.0,
            negativeMarks = 0.25,
            durationMinutes = "15",
            questionCount = 5
        )

        val questions = listOf(
            Question(
                rowId = "q1",
                questionId = 1,
                questionText = "Which of the following is the therapeutic serum concentration range for Digoxin?",
                optionA = "0.5 – 2.0 ng/mL",
                optionB = "3.0 – 5.0 ng/mL",
                optionC = "10 – 20 mcg/mL",
                optionD = "0.1 – 0.4 mg/dL",
                correctAnswer = "A",
                explanation = "The therapeutic range for Digoxin is 0.5 to 2.0 ng/mL. Levels exceeding 2.0 ng/mL can lead to Digoxin toxicity, characterized by anorexia, nausea, vomiting, visual halos, and bradycardia.",
                questionNo = 1
            ),
            Question(
                rowId = "q2",
                questionId = 2,
                questionText = "A patient is prescribed Morphine, Oxygen, Nitroglycerin, and Aspirin (MONA) for suspected Myocardial Infarction. Which nursing assessment is CRITICAL before administering Nitroglycerin?",
                optionA = "Check respiratory rate",
                optionB = "Measure Blood Pressure (Systolic BP)",
                optionC = "Assess blood glucose level",
                optionD = "Check serum potassium",
                correctAnswer = "B",
                explanation = "Nitroglycerin is a potent vasodilator that lowers BP. It should be withheld if Systolic Blood Pressure is below 90 mmHg to prevent severe hypotension and shock.",
                questionNo = 2
            ),
            Question(
                rowId = "q3",
                questionId = 3,
                questionText = "In Bio-Medical Waste Management in India, human anatomical waste such as body tissues and organs must be disposed of in which color-coded container?",
                optionA = "Red Bag",
                optionB = "Yellow Bag",
                optionC = "Blue Container",
                optionD = "White Translucent Container",
                correctAnswer = "B",
                explanation = "Human anatomical waste, soiled cotton/dressings, and expired medicines are disposed of in YELLOW bags for incineration or plasma pyrolysis.",
                questionNo = 3
            ),
            Question(
                rowId = "q4",
                questionId = 4,
                questionText = "What is the primary pacemaker of the human heart?",
                optionA = "Atrioventricular (AV) Node",
                optionB = "Sinoatrial (SA) Node",
                optionC = "Bundle of His",
                optionD = "Purkinje Fibers",
                correctAnswer = "B",
                explanation = "The Sinoatrial (SA) node located in the right atrium generates spontaneous electrical impulses at 60-100 beats per minute, making it the primary pacemaker.",
                questionNo = 4
            ),
            Question(
                rowId = "q5",
                questionId = 5,
                questionText = "Which drug is the antidote of choice for Heparin toxicity?",
                optionA = "Vitamin K",
                optionB = "Protamine Sulfate",
                optionC = "Naloxone",
                optionD = "Calcium Gluconate",
                correctAnswer = "B",
                explanation = "Protamine Sulfate neutralizes heparin by forming a stable salt complex. Vitamin K is the antidote for Warfarin.",
                questionNo = 5
            )
        )

        return Pair(quiz, questions)
    }

    private fun getFallbackSliders(): List<SliderItem> {
        return listOf(
            SliderItem(
                id = 1,
                rowId = "sl_01",
                imageUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=1000",
                name = "NORCET 8.0 Live Batch Announced!",
                description = "Daily live interactive lectures, downloadable notes, and weekly mock exams."
            ),
            SliderItem(
                id = 2,
                rowId = "sl_02",
                imageUrl = "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=1000",
                name = "ESIC 2026 Special Test Series",
                description = "Practice 50+ subject-wise and grand mock test series with real negative marking."
            )
        )
    }
}
