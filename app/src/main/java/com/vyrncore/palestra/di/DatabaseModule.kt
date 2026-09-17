package com.vyrncore.palestra.di

import android.content.Context
import androidx.room.Room
import com.vyrncore.palestra.data.local.AppDatabase
import com.vyrncore.palestra.data.local.MIGRATION_3_4
import com.vyrncore.palestra.data.local.MIGRATION_4_5
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
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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
}
