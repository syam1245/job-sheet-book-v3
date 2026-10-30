package com.example.jobsheetbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.jobsheetbook.data.local.dao.CustomerDao
import com.example.jobsheetbook.data.local.dao.JobDao
import com.example.jobsheetbook.data.local.dao.JobEventDao
import com.example.jobsheetbook.data.local.entities.CustomerEntity
import com.example.jobsheetbook.data.local.entities.JobEntity
import com.example.jobsheetbook.data.local.entities.JobEventEntity

@Database(
    entities = [CustomerEntity::class, JobEntity::class, JobEventEntity::class],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun jobDao(): JobDao
    abstract fun jobEventDao(): JobEventDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE jobs ADD COLUMN estimatedCost INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE jobs ADD COLUMN finalCost INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE jobs ADD COLUMN displayJobId TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE jobs ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
