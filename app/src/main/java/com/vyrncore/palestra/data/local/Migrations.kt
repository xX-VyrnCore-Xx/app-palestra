package com.vyrncore.palestra.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Chat attachments (link/file/image) and richer workout plan metadata (category, estimated
 * duration). Written by hand rather than relying on [androidx.room.Room.databaseBuilder]'s
 * fallbackToDestructiveMigration, so upgrading the app no longer wipes a user's local data.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentUrl TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentName TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentType TEXT")
        db.execSQL("ALTER TABLE workout_plans ADD COLUMN category TEXT")
        db.execSQL("ALTER TABLE workout_plans ADD COLUMN estimatedMinutes INTEGER")
    }
}

/** PT-recorded injuries/limitations per client, surfaced wherever the PT builds a plan. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profiles ADD COLUMN injuries TEXT")
    }
}

/** Optional demonstrative image/GIF URL per exercise. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN imageUrl TEXT")
    }
}

/** Multi-week structured programs (mesocicli): each week is a regular workout_plans row tagged
 * with its program and week number, so nothing else in the plan pipeline needs to change. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_plans ADD COLUMN programId TEXT")
        db.execSQL("ALTER TABLE workout_plans ADD COLUMN weekIndex INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_plans_programId ON workout_plans(programId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS programs (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                createdByPtId TEXT NOT NULL,
                assignedToUserId TEXT NOT NULL,
                totalWeeks INTEGER NOT NULL,
                weeklyIncrementPercent REAL NOT NULL,
                startEpochMs INTEGER NOT NULL,
                syncStatus TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_programs_createdByPtId ON programs(createdByPtId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_programs_assignedToUserId ON programs(assignedToUserId)")
    }
}

/** Optional user-chosen profile picture, uploaded to the "avatars" storage bucket. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profiles ADD COLUMN avatarUrl TEXT")
    }
}

/** Soft-delete flag for chat messages: a deleted message keeps its row (as a tombstone) instead
 * of leaving a confusing gap in the conversation. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

/** Quick freeform note the PT can leave on a single exercise within a plan. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plan_exercises ADD COLUMN notes TEXT")
    }
}
