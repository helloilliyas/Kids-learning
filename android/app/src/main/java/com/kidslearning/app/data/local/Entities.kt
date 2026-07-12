package com.kidslearning.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lean local-first schema.
 *
 * Deliberately NOT one table per lesson concept. The lesson already has a strict
 * JSON schema, so the whole lesson is stored as a single JSON document
 * ([LessonEntity.lessonJson]). Versioning an extended lesson is just saving a new
 * row with an incremented [version]. Only the things we actually query and
 * aggregate are normalised: attempts (for progress) and per-concept mastery.
 *
 * This is five tables instead of eleven, with far less mapping code, and it keeps
 * the JSON schema as the single source of truth for lesson shape.
 */

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val sourceId: String,
    val title: String,
    val localUri: String,   // path to the original file, for "View Source"
    val mimeType: String,
    val createdAt: Long,
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val lessonId: String,
    val version: Int,               // extensions bump this; older versions retained
    val title: String,
    val subject: String,
    val age: Int,
    val approved: Boolean,          // parent must approve before child mode shows it
    val schemaVersion: String,
    val lessonJson: String,         // the full Lesson document, verbatim
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String,
    val sectionId: String,
    val conceptId: String?,
    val correct: Boolean,
    val attempts: Int,
    val usedHint: Boolean,
    val timeSpentMs: Long,
    val answeredAt: Long,
)

@Entity(tableName = "concept_mastery", primaryKeys = ["lessonId", "conceptId"])
data class ConceptMasteryEntity(
    val lessonId: String,
    val conceptId: String,
    val mastery: Double,
    val consecutiveCorrect: Int,
    val consecutiveWrong: Int,
    val updatedAt: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 0,    // single-row table
    val parentPinHash: String?,     // gate for parent mode
    val readAloudDefault: Boolean,
    val backendBaseUrl: String,
)
