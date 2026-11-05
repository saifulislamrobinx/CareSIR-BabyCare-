package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BabyProfileEntity::class,
        GrowthRecordEntity::class,
        FeedingRecordEntity::class,
        DiaperRecordEntity::class,
        HygieneRecordEntity::class,
        EquipmentCleaningRecordEntity::class,
        CustomReminderEntity::class,
        ReminderCompletionEntity::class,
        NotificationSettingsEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CareSirDatabase : RoomDatabase() {
    abstract fun careSirDao(): CareSirDao

    companion object {
        @Volatile
        private var INSTANCE: CareSirDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Reserved for future schema expansion (Vaccination, Sleep, Milestones)
            }
        }

        fun getInstance(context: Context): CareSirDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CareSirDatabase::class.java,
                    "caresir_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
