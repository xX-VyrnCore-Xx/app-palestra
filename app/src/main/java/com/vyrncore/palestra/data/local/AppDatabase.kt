package com.vyrncore.palestra.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vyrncore.palestra.data.local.dao.BodyMetricDao
import com.vyrncore.palestra.data.local.dao.ChatMessageDao
import com.vyrncore.palestra.data.local.dao.ExerciseDao
import com.vyrncore.palestra.data.local.dao.PlanExerciseDao
import com.vyrncore.palestra.data.local.dao.ProgramDao
import com.vyrncore.palestra.data.local.dao.PtNoteDao
import com.vyrncore.palestra.data.local.dao.SetEntryDao
import com.vyrncore.palestra.data.local.dao.UserProfileDao
import com.vyrncore.palestra.data.local.dao.WorkoutPlanDao
import com.vyrncore.palestra.data.local.dao.WorkoutSessionDao
import com.vyrncore.palestra.data.local.entity.BodyMetricEntity
import com.vyrncore.palestra.data.local.entity.ChatMessageEntity
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.data.local.entity.PlanExerciseEntity
import com.vyrncore.palestra.data.local.entity.ProgramEntity
import com.vyrncore.palestra.data.local.entity.PtNoteEntity
import com.vyrncore.palestra.data.local.entity.SetEntryEntity
import com.vyrncore.palestra.data.local.entity.UserProfileEntity
import com.vyrncore.palestra.data.local.entity.WorkoutPlanEntity
import com.vyrncore.palestra.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        PlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetEntryEntity::class,
        BodyMetricEntity::class,
        ChatMessageEntity::class,
        PtNoteEntity::class,
        ProgramEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun planExerciseDao(): PlanExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setEntryDao(): SetEntryDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun ptNoteDao(): PtNoteDao
    abstract fun programDao(): ProgramDao

    companion object {
        const val DATABASE_NAME = "palestra.db"
    }
}
