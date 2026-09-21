package com.vyrncore.palestra.di

import android.content.Context
import androidx.room.Room
import com.vyrncore.palestra.data.local.AppDatabase
import com.vyrncore.palestra.data.local.MIGRATION_10_11
import com.vyrncore.palestra.data.local.MIGRATION_11_12
import com.vyrncore.palestra.data.local.MIGRATION_12_13
import com.vyrncore.palestra.data.local.MIGRATION_13_14
import com.vyrncore.palestra.data.local.MIGRATION_3_4
import com.vyrncore.palestra.data.local.MIGRATION_4_5
import com.vyrncore.palestra.data.local.MIGRATION_5_6
import com.vyrncore.palestra.data.local.MIGRATION_6_7
import com.vyrncore.palestra.data.local.MIGRATION_7_8
import com.vyrncore.palestra.data.local.MIGRATION_8_9
import com.vyrncore.palestra.data.local.MIGRATION_9_10
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(
                MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
            )
            // NB: fallback kept deliberately — very old installs (pre-v3 schema) have no
            // migration path, and wiping is preferable to crashing on upgrade.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserProfileDao(db: AppDatabase) = db.userProfileDao()

    @Provides
    fun provideExerciseDao(db: AppDatabase) = db.exerciseDao()

    @Provides
    fun provideWorkoutPlanDao(db: AppDatabase) = db.workoutPlanDao()

    @Provides
    fun providePlanExerciseDao(db: AppDatabase) = db.planExerciseDao()

    @Provides
    fun provideWorkoutSessionDao(db: AppDatabase) = db.workoutSessionDao()

    @Provides
    fun provideSetEntryDao(db: AppDatabase) = db.setEntryDao()

    @Provides
    fun provideBodyMetricDao(db: AppDatabase) = db.bodyMetricDao()

    @Provides
    fun provideChatMessageDao(db: AppDatabase) = db.chatMessageDao()

    @Provides
    fun providePtNoteDao(db: AppDatabase) = db.ptNoteDao()

    @Provides
    fun provideProgramDao(db: AppDatabase) = db.programDao()

    @Provides
    fun providePlanTemplateDao(db: AppDatabase) = db.planTemplateDao()

    @Provides
    fun providePlanTemplateExerciseDao(db: AppDatabase) = db.planTemplateExerciseDao()
}
