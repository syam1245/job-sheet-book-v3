package com.example.jobsheetbook.di

import android.content.Context
import androidx.room.Room
import com.example.jobsheetbook.data.local.AppDatabase
import com.example.jobsheetbook.data.local.dao.CustomerDao
import com.example.jobsheetbook.data.local.dao.JobDao
import com.example.jobsheetbook.data.local.dao.JobEventDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "jobsheet_db"
        )
        .addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9)
        .build()
    }

    @Provides
    fun provideCustomerDao(database: AppDatabase): CustomerDao {
        return database.customerDao()
    }

    @Provides
    fun provideJobDao(database: AppDatabase): JobDao {
        return database.jobDao()
    }

    @Provides
    fun provideJobEventDao(database: AppDatabase): JobEventDao {
        return database.jobEventDao()
    }
}
