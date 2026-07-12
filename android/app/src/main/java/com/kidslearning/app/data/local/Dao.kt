package com.kidslearning.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Upsert
    suspend fun upsertLesson(lesson: LessonEntity)

    @Query("SELECT * FROM lessons ORDER BY updatedAt DESC")
    fun observeLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE lessonId = :id ORDER BY version DESC LIMIT 1")
    suspend fun latestVersion(id: String): LessonEntity?

    @Query("UPDATE lessons SET approved = 1, updatedAt = :now WHERE lessonId = :id")
    suspend fun approve(id: String, now: Long)
}

/** Per-lesson count of distinct correctly-solved activities, for the home list. */
data class SolvedCount(val lessonId: String, val solved: Int)

@Dao
interface ProgressDao {
    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Upsert
    suspend fun upsertMastery(mastery: ConceptMasteryEntity)

    @Query("SELECT * FROM concept_mastery WHERE lessonId = :lessonId AND conceptId = :conceptId")
    suspend fun getMastery(lessonId: String, conceptId: String): ConceptMasteryEntity?

    @Query("SELECT * FROM concept_mastery WHERE lessonId = :lessonId")
    fun observeMastery(lessonId: String): Flow<List<ConceptMasteryEntity>>

    @Query("SELECT * FROM concept_mastery WHERE lessonId = :lessonId AND mastery < 0.5")
    suspend fun weakConcepts(lessonId: String): List<ConceptMasteryEntity>

    @Query(
        "SELECT lessonId, COUNT(DISTINCT sectionId) AS solved FROM attempts " +
            "WHERE correct = 1 GROUP BY lessonId"
    )
    fun observeSolvedCounts(): Flow<List<SolvedCount>>
}

@Dao
interface SourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: SourceEntity)

    @Query("SELECT * FROM sources WHERE sourceId = :id")
    suspend fun get(id: String): SourceEntity?
}

@Database(
    entities = [
        SourceEntity::class,
        LessonEntity::class,
        AttemptEntity::class,
        ConceptMasteryEntity::class,
        AppSettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun progressDao(): ProgressDao
    abstract fun sourceDao(): SourceDao
}
