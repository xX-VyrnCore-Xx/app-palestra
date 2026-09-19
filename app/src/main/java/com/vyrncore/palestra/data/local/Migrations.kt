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
