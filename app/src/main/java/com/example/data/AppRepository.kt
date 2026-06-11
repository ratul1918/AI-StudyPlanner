package com.example.data

import android.util.Log
import com.example.data.database.NoteEntity
import com.example.data.database.StudyDao
import com.example.data.database.UserEntity
import com.example.data.database.SummaryEntity
import com.example.data.database.QuizEntity
import com.example.data.database.FlashcardEntity
import com.example.data.database.StudyPlanEntity
import com.example.data.database.ProgressEntity
import com.example.data.api.GeminiApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(private val studyDao: StudyDao) {

    // --- Active User Sessions ---
    suspend fun getUser(userId: String): UserEntity? {
        return studyDao.getUserById(userId)
    }

    suspend fun saveUser(user: UserEntity) {
        studyDao.insertUser(user)
    }

    // --- Notes Management ---
    fun getNotesFlow(userId: String): Flow<List<NoteEntity>> = studyDao.getNotesByUser(userId)

    suspend fun addNote(userId: String, title: String, contentText: String, folderName: String = "All Notes", fileUrl: String = "local"): Long {
        val note = NoteEntity(
            userId = userId,
            title = title,
            fileUrl = fileUrl,
            folderName = folderName,
            contentText = contentText
        )
        return studyDao.insertNote(note)
    }

    suspend fun updateNote(noteId: Long, title: String, folderName: String) {
        studyDao.updateNote(noteId, title, folderName)
    }

    suspend fun deleteNote(noteId: Long) {
        studyDao.deleteNoteById(noteId)
    }

    // --- Summaries ---
    fun getSummaryFlow(noteId: Long): Flow<SummaryEntity?> = studyDao.getSummaryFlowForNote(noteId)

    suspend fun fetchOrGenerateSummary(noteId: Long, title: String, contentText: String, formatType: String): SummaryEntity {
        val existing = studyDao.getSummaryForNote(noteId)
        if (existing != null) {
            // Check if we need to regenerate/update specific summary parts
            val updated = when (formatType.lowercase()) {
                "chapter" -> existing.copy(chapterSummary = GeminiApi.generateSummary(title, contentText, "Chapter"))
                "topic" -> existing.copy(topicSummary = GeminiApi.generateSummary(title, contentText, "Topic"))
                "bullet" -> existing.copy(bulletSummary = GeminiApi.generateSummary(title, contentText, "Bullet Point"))
                "exam" -> existing.copy(examSummary = GeminiApi.generateSummary(title, contentText, "Exam-Oriented"))
                else -> existing
            }
            studyDao.insertSummary(updated)
            return updated
        } else {
            // Generate all or format-specific and placeholder for other formats
            val summaryText = GeminiApi.generateSummary(title, contentText, formatType)
            val newSummary = when (formatType.lowercase()) {
                "chapter" -> SummaryEntity(noteId = noteId, chapterSummary = summaryText, topicSummary = "", bulletSummary = "", examSummary = "")
                "topic" -> SummaryEntity(noteId = noteId, chapterSummary = "", topicSummary = summaryText, bulletSummary = "", examSummary = "")
                "bullet" -> SummaryEntity(noteId = noteId, chapterSummary = "", topicSummary = "", bulletSummary = summaryText, examSummary = "")
                "exam" -> SummaryEntity(noteId = noteId, chapterSummary = "", topicSummary = "", bulletSummary = "", examSummary = summaryText)
                else -> SummaryEntity(noteId = noteId, chapterSummary = summaryText, topicSummary = "", bulletSummary = "", examSummary = "")
            }
            studyDao.insertSummary(newSummary)
            return newSummary
        }
    }

    // --- Quizzes ---
    fun getQuizzesFlow(noteId: Long): Flow<List<QuizEntity>> = studyDao.getQuizzesForNote(noteId)
    fun getAllQuizzesFlow(): Flow<List<QuizEntity>> = studyDao.getAllQuizzes()

    suspend fun generateQuizForNote(noteId: Long, noteTitle: String, noteContent: String): QuizEntity {
        val quizJson = GeminiApi.generateQuiz(noteTitle, noteContent)
        val quiz = QuizEntity(
            noteId = noteId,
            quizTitle = "$noteTitle - AI Quiz",
            quizDataJson = quizJson
        )
        studyDao.insertQuiz(quiz)
        return quiz
    }

    // --- Flashcards ---
    fun getFlashcardsFlow(noteId: Long): Flow<List<FlashcardEntity>> = studyDao.getFlashcardsForNote(noteId)
    fun getAllFlashcardsFlow(): Flow<List<FlashcardEntity>> = studyDao.getAllFlashcardsFlow()

    suspend fun addManualFlashcard(noteId: Long, question: String, answer: String) {
        val flash = FlashcardEntity(
            noteId = noteId,
            question = question,
            answer = answer,
            isManual = true
        )
        studyDao.insertFlashcard(flash)
    }

    suspend fun generateFlashcardsForNote(noteId: Long, noteTitle: String, noteContent: String): List<FlashcardEntity> {
        val jsonStr = GeminiApi.generateFlashcards(noteTitle, noteContent)
        val list = mutableListOf<FlashcardEntity>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val f = FlashcardEntity(
                    noteId = noteId,
                    question = obj.optString("question", "No question?"),
                    answer = obj.optString("answer", "No answer?"),
                    isManual = false
                )
                studyDao.insertFlashcard(f)
                list.add(f)
            }
        } catch (e: Exception) {
            // Fallback manual parse if format contains weird items
            Log.e("Repository", "Flashcard JSON parse exception", e)
        }
        return list
    }

    suspend fun updateFlashcardMastery(id: Long, isMastered: Boolean) {
        studyDao.updateFlashcardMastery(id, isMastered)
    }

    suspend fun deleteFlashcard(id: Long) {
        studyDao.deleteFlashcardById(id)
    }

    // --- Study Plans ---
    fun getStudyPlansFlow(userId: String): Flow<List<StudyPlanEntity>> = studyDao.getStudyPlans(userId)

    suspend fun createStudyPlan(userId: String, subject: String, examDate: String, hoursPerDay: Float) {
        val planJson = GeminiApi.generateStudyPlan(subject, examDate, hoursPerDay)
        val plan = StudyPlanEntity(
            userId = userId,
            subject = subject,
            examDate = examDate,
            studyHours = hoursPerDay,
            planDataJson = planJson
        )
        studyDao.insertStudyPlan(plan)
    }

    suspend fun deleteStudyPlan(planId: Long) {
        studyDao.deleteStudyPlanById(planId)
    }

    // --- Progress Tracking ---
    fun getProgressFlow(userId: String): Flow<List<ProgressEntity>> = studyDao.getProgressByWeek(userId)

    suspend fun recordStudySession(userId: String, dateStr: String, minutes: Int, accuracy: Float = 0.0f, subject: String = "") {
        // Construct a clean entry, combining with any existing entry for that day
        val existingList = studyDao.getProgressByWeek(userId).firstOrNull() ?: emptyList()
        val match = existingList.find { it.dateString == dateStr }
        val finalProgress = if (match != null) {
            val updatedMinutes = match.studyMinutes + minutes
            val updatedAccuracy = if (accuracy > 0) accuracy else match.quizAccuracy
            val currentMap = try {
                val obj = JSONObject(match.subjectProgressJson)
                if (subject.isNotEmpty()) {
                    obj.put(subject, (obj.optDouble(subject, 0.0) + 10.0).coerceAtMost(100.0))
                }
                obj.toString()
            } catch (e: Exception) {
                "{}"
            }
            match.copy(studyMinutes = updatedMinutes, quizAccuracy = updatedAccuracy, subjectProgressJson = currentMap)
        } else {
            val mapStr = if (subject.isNotEmpty()) "{\"$subject\": 10.0}" else "{}"
            ProgressEntity(
                userId = userId,
                dateString = dateStr,
                studyMinutes = minutes,
                quizAccuracy = accuracy,
                subjectProgressJson = mapStr
            )
        }
        studyDao.insertProgress(finalProgress)
    }

    // --- OCR & Document Verification helper ---
    suspend fun runOcrOnBinary(mimeType: String, imageBase64: String): String {
        return GeminiApi.performOcr(mimeType, imageBase64)
    }

    suspend fun analyzePapers(title: String, content: String): String {
        return GeminiApi.analyzeResearchPaper(title, content)
    }

    // --- DB Prep-Population (WOW Touch!) ---
    suspend fun prepopulateDefaultData(userId: String) {
        val currentNotesFlow = studyDao.getNotesByUser(userId)
        val list = currentNotesFlow.firstOrNull() ?: emptyList()
        if (list.isEmpty()) {
            // Note 1: Operating Systems & Scheduling
            val note1Id = addNote(
                userId = userId,
                title = "Operating Systems: CPU Scheduling Algorithms",
                contentText = """
                    CPU Scheduling is a process that allows one process to use the CPU while the execution of another process is on hold (in waiting state) due to unavailability of any resource like I/O etc., thereby making full use of CPU.
                    
                    The scheduling concepts include:
                    1. FCFS (First Come First Serve): It is non-preemptive. Simple, but suffers from the Convoy Effect, where short jobs wait for long jobs to finish.
                    2. SJF (Shortest Job First): Optimal but hard to estimate next CPU burst time. Can be preemptive (SRTF) or non-preemptive.
                    3. Round Robin (RR): Preemptive algorithm that allocates a tiny fraction of time called 'Time Quantum' or 'Time Slice' (usually 10-100ms) to each process sequentially. Extremely responsive for timesharing, but depends heavily on correct time quantum size. If quantum is too short, context-switch overhead gets huge.
                    4. Priority Scheduling: Allocates CPU based on static/dynamic priority weights. Suffer from Starvation (indefinite blocking), solved using Aging (gradually increasing priority of waiting jobs).
                    
                    Deadlock definition: A state where a set of processes are blocked because each process is holding a resource and waiting for another resource acquired by some other process.
                    Four necessary conditions for a deadlock to exist:
                    - Mutual Exclusion
                    - Hold and Wait
                    - No Preemption
                    - Circular Wait
                """.trimIndent(),
                folderName = "Computer Science",
                fileUrl = "virtual_lecture_1.pdf"
            )

            // Save standard sample summary for note 1
            studyDao.insertSummary(
                SummaryEntity(
                    noteId = note1Id,
                    chapterSummary = "This document presents the core mechanics of UNIX and General-Purpose OS architecture focusing on CPU scheduling optimization. The main bottleneck is maximizing CPU utilization and core throughput. Four core algorithms are evaluated: FCFS, Shortest Job First (SJF), Round Robin (using a time quantum), and priority metrics. It also identifies Deadlocks, outlining the four mandatory conditions for occurrence: Mutual Exclusion, Hold and Wait, No Preemption, Circular Wait.",
                    topicSummary = "• CPU Scheduling: Maximizing core resource utilization by multiplexing execution contexts.\n• Time Quantum: In Round Robin scheduling, the allocated execution block before a context switch is enforced.\n• Deadlock: Mutual blocking state where resources cannot be released due to circular waiting conditions.",
                    bulletSummary = "• CPU Scheduling coordinates which active thread receives core execution slices.\n• FCFS is non-preemptive and prone to the Convoy Effect.\n• SJF is mathematically optimal but requires estimating future processing bursts.\n• Round Robin is tailored for interactive multi-user timesharing.\n• Deadlocks require four co-occurring processes: mutual exclusion, hold & wait, no preemption, and circular wait.",
                    examSummary = "★ Core Formula / Rubric:\n1. Context-switching latency is overhead times context switch frequency.\n2. Standard Deadlock Prevention includes breaking Circular Wait (e.g., hierarchical resource locking).\n\n★ Frequently Tested:\nQ: Explain the Convoy Effect under FCFS.\nQ: List the four Coffman deadlock conditions."
                )
            )

            // Note 2: Machine Learning
            val note2Id = addNote(
                userId = userId,
                title = "Machine Learning: Supervised vs Unsupervised Models",
                contentText = """
                    Machine Learning is classified into three branches: Supervised, Unsupervised, and Reinforcement Learning.
                    
                    1. Supervised Learning: Evaluates mapping inputs to labeled targets.
                       - Classification: Discretized class boundaries (e.g., Logistic Regression, Support Vector Machines, Decision Trees).
                       - Regression: Continuous real values (e.g., Linear Regression, Neural Network regressors).
                       
                    2. Unsupervised Learning: Focuses on unlabelled target variables to discover natural underlying cluster densities.
                       - Clustering: K-Means, DBSCAN, Hierarchical Clustering.
                       - Dimensionality Reduction: PCA (Principal Component Analysis), t-SNE, Autoencoders.
                       
                    3. Key Metrics:
                       - Precision: True Positives / (True Positives + False Positives)
                       - Recall (Sensitivity): True Positives / (True Positives + False Negatives)
                       - Overfitting: Over-indexing on training noise, leading to poor validation generalizability. Controlled via L1/L2 Regularization.
                """.trimIndent(),
                folderName = "Computer Science",
                fileUrl = "machine_learning_basics.pdf"
            )

            // Generate progress entries so graphs are loaded!
            recordStudySession(userId, "2026-06-08", 45, 80.0f, "Computer Science")
            recordStudySession(userId, "2026-06-09", 60, 90.0f, "Computer Science")
            recordStudySession(userId, "2026-06-10", 30, 75.0f, "Computer Science")
            recordStudySession(userId, "2026-06-11", 50, 85.0f, "Computer Science")

            // Prepopulate an active Study Plan
            val defaultPlanJson = """
                {
                  "subject": "Operating Systems",
                  "examCountdownDays": 7,
                  "schedule": [
                    {
                      "day": "Day 1",
                      "focusTopic": "Core Terminology & Introductions",
                      "allocatedHours": 2.0,
                      "activities": ["Read core chapters", "Establish active recall definitions"],
                      "status": "Completed"
                    },
                    {
                      "day": "Day 2",
                      "focusTopic": "Review Mathematical formulas and Equations",
                      "allocatedHours": 2.0,
                      "activities": ["Do sample proofs", "Attempt flashcard retention trials"],
                      "status": "Completed"
                    },
                    {
                      "day": "Day 3",
                      "focusTopic": "Detailed MCQ Practice Run",
                      "allocatedHours": 3.0,
                      "activities": ["Generate AI Quiz", "Solve 20 past exam issues"],
                      "status": "Pending"
                    },
                    {
                      "day": "Day 4",
                      "focusTopic": "Comprehensive final Revision",
                      "allocatedHours": 1.5,
                      "activities": ["Flashcards Mastery overview", "Relax and rest"],
                      "status": "Pending"
                    }
                  ]
                }
            """.trimIndent()
            studyDao.insertStudyPlan(
                StudyPlanEntity(
                    userId = userId,
                    subject = "Operating Systems",
                    examDate = "2026-06-18",
                    studyHours = 2.0f,
                    planDataJson = defaultPlanJson
                )
            )
        }
    }

    suspend fun chatWithNotes(noteTitle: String, noteContent: String, userQuestion: String, chatHistoryJson: String): String {
        return GeminiApi.chatWithNotes(noteTitle, noteContent, userQuestion, chatHistoryJson)
    }

    suspend fun insertFlashcardEntity(flashcard: FlashcardEntity) {
        studyDao.insertFlashcard(flashcard)
    }

    suspend fun saveQuizEntity(noteId: Long, title: String, json: String) {
        val q = QuizEntity(noteId = noteId, quizTitle = title, quizDataJson = json)
        studyDao.insertQuiz(q)
    }
}
