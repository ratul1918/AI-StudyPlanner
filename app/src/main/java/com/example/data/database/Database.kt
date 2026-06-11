package com.example.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val photoUrl: String
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val noteId: Long = 0,
    val userId: String,
    val title: String,
    val fileUrl: String, // "local" or virtual path
    val folderName: String = "All Notes",
    val uploadedAt: Long = System.currentTimeMillis(),
    val contentText: String
)

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true) val summaryId: Long = 0,
    val noteId: Long,
    val chapterSummary: String,
    val topicSummary: String,
    val bulletSummary: String,
    val examSummary: String
)

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey(autoGenerate = true) val quizId: Long = 0,
    val noteId: Long,
    val quizTitle: String,
    val quizDataJson: String // Stringified JSON holding MCQs, True/False and short questions
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val flashcardId: Long = 0,
    val noteId: Long,
    val question: String,
    val answer: String,
    val isMastered: Boolean = false,
    val isManual: Boolean = false
)

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true) val planId: Long = 0,
    val userId: String,
    val subject: String,
    val examDate: String,
    val studyHours: Float,
    val planDataJson: String // Day-by-day plan in JSON format
)

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey(autoGenerate = true) val progressId: Long = 0,
    val userId: String,
    val dateString: String, // YYYY-MM-DD
    val studyMinutes: Int,
    val quizAccuracy: Float, // percentage e.g. 85.0f
    val subjectProgressJson: String // JSON map of subject -> progress
)

// --- DAO (Data Access Object) ---

@Dao
interface StudyDao {
    // Users
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Notes
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY uploadedAt DESC")
    fun getNotesByUser(userId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE noteId = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("UPDATE notes SET title = :title, folderName = :folder WHERE noteId = :noteId")
    suspend fun updateNote(noteId: Long, title: String, folder: String)

    // Summaries
    @Query("SELECT * FROM summaries WHERE noteId = :noteId LIMIT 1")
    suspend fun getSummaryForNote(noteId: Long): SummaryEntity?

    @Query("SELECT * FROM summaries WHERE noteId = :noteId LIMIT 1")
    fun getSummaryFlowForNote(noteId: Long): Flow<SummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)

    // Quizzes
    @Query("SELECT * FROM quizzes WHERE noteId = :noteId")
    fun getQuizzesForNote(noteId: Long): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes ORDER BY quizId DESC")
    fun getAllQuizzes(): Flow<List<QuizEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    // Flashcards
    @Query("SELECT * FROM flashcards WHERE noteId = :noteId ORDER BY flashcardId DESC")
    fun getFlashcardsForNote(noteId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY flashcardId DESC")
    fun getAllFlashcardsFlow(): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)

    @Query("UPDATE flashcards SET isMastered = :isMastered WHERE flashcardId = :id")
    suspend fun updateFlashcardMastery(id: Long, isMastered: Boolean)

    @Query("DELETE FROM flashcards WHERE flashcardId = :id")
    suspend fun deleteFlashcardById(id: Long)

    // Study Plans
    @Query("SELECT * FROM study_plans WHERE userId = :userId ORDER BY planId DESC")
    fun getStudyPlans(userId: String): Flow<List<StudyPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPlan(plan: StudyPlanEntity)

    @Query("DELETE FROM study_plans WHERE planId = :planId")
    suspend fun deleteStudyPlanById(planId: Long)

    // Progress
    @Query("SELECT * FROM progress WHERE userId = :userId ORDER BY dateString ASC")
    fun getProgressByWeek(userId: String): Flow<List<ProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressEntity)
}

// --- AppDatabase ---

@Database(
    entities = [
        UserEntity::class,
        NoteEntity::class,
        SummaryEntity::class,
        QuizEntity::class,
        FlashcardEntity::class,
        StudyPlanEntity::class,
        ProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_study_assistant_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
