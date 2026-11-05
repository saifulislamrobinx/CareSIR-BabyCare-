package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CareSirDao {

    // 1. BabyProfile
    @Query("SELECT * FROM baby_profiles ORDER BY isActive DESC, id ASC")
    fun observeAllBabies(): Flow<List<BabyProfileEntity>>

    @Query("SELECT * FROM baby_profiles ORDER BY isActive DESC, id ASC")
    suspend fun getAllBabiesOnce(): List<BabyProfileEntity>

    @Query("SELECT * FROM baby_profiles WHERE isActive = 1 LIMIT 1")
    fun observeActiveBaby(): Flow<BabyProfileEntity?>

    @Query("SELECT * FROM baby_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveBabyOnce(): BabyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBabyProfile(profile: BabyProfileEntity): Long

    @Update
    suspend fun updateBabyProfile(profile: BabyProfileEntity)

    @Query("UPDATE baby_profiles SET isActive = CASE WHEN id = :babyId THEN 1 ELSE 0 END, updatedAt = :now")
    suspend fun setActiveBaby(babyId: Long, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteBabyProfile(profile: BabyProfileEntity)

    // 2. GrowthRecord
    @Query("SELECT * FROM growth_records WHERE babyId = :babyId ORDER BY recordedAt DESC")
    fun observeGrowthRecords(babyId: Long): Flow<List<GrowthRecordEntity>>

    @Query("SELECT * FROM growth_records ORDER BY recordedAt DESC")
    suspend fun getAllGrowthRecordsOnce(): List<GrowthRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrowthRecord(record: GrowthRecordEntity): Long

    @Delete
    suspend fun deleteGrowthRecord(record: GrowthRecordEntity)

    // 3. FeedingRecord
    @Query("SELECT * FROM feeding_records WHERE babyId = :babyId ORDER BY startTimeMillis DESC")
    fun observeFeedingRecords(babyId: Long): Flow<List<FeedingRecordEntity>>

    @Query("SELECT * FROM feeding_records ORDER BY startTimeMillis DESC")
    suspend fun getAllFeedingRecordsOnce(): List<FeedingRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedingRecord(record: FeedingRecordEntity): Long

    @Update
    suspend fun updateFeedingRecord(record: FeedingRecordEntity)

    @Delete
    suspend fun deleteFeedingRecord(record: FeedingRecordEntity)

    // 4. DiaperRecord
    @Query("SELECT * FROM diaper_records WHERE babyId = :babyId ORDER BY timestampMillis DESC")
    fun observeDiaperRecords(babyId: Long): Flow<List<DiaperRecordEntity>>

    @Query("SELECT * FROM diaper_records ORDER BY timestampMillis DESC")
    suspend fun getAllDiaperRecordsOnce(): List<DiaperRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaperRecord(record: DiaperRecordEntity): Long

    @Update
    suspend fun updateDiaperRecord(record: DiaperRecordEntity)

    @Delete
    suspend fun deleteDiaperRecord(record: DiaperRecordEntity)

    // 5. HygieneRecord
    @Query("SELECT * FROM hygiene_records WHERE babyId = :babyId ORDER BY timestampMillis DESC")
    fun observeHygieneRecords(babyId: Long): Flow<List<HygieneRecordEntity>>

    @Query("SELECT * FROM hygiene_records ORDER BY timestampMillis DESC")
    suspend fun getAllHygieneRecordsOnce(): List<HygieneRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHygieneRecord(record: HygieneRecordEntity): Long

    @Update
    suspend fun updateHygieneRecord(record: HygieneRecordEntity)

    @Delete
    suspend fun deleteHygieneRecord(record: HygieneRecordEntity)

    // 6. EquipmentCleaningRecord
    @Query("SELECT * FROM equipment_cleaning_records WHERE babyId = :babyId ORDER BY completedAtMillis DESC")
    fun observeEquipmentCleaningRecords(babyId: Long): Flow<List<EquipmentCleaningRecordEntity>>

    @Query("SELECT * FROM equipment_cleaning_records ORDER BY completedAtMillis DESC")
    suspend fun getAllEquipmentCleaningRecordsOnce(): List<EquipmentCleaningRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipmentCleaningRecord(record: EquipmentCleaningRecordEntity): Long

    @Update
    suspend fun updateEquipmentCleaningRecord(record: EquipmentCleaningRecordEntity)

    @Delete
    suspend fun deleteEquipmentCleaningRecord(record: EquipmentCleaningRecordEntity)

    // 7. CustomReminder
    @Query("SELECT * FROM custom_reminders WHERE babyId = :babyId ORDER BY isCompleted ASC, scheduledTimeMillis ASC")
    fun observeCustomReminders(babyId: Long): Flow<List<CustomReminderEntity>>

    @Query("SELECT * FROM custom_reminders ORDER BY scheduledTimeMillis ASC")
    suspend fun getAllCustomRemindersOnce(): List<CustomReminderEntity>

    @Query("SELECT * FROM custom_reminders WHERE id = :id LIMIT 1")
    suspend fun getCustomReminderById(id: Long): CustomReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomReminder(reminder: CustomReminderEntity): Long

    @Update
    suspend fun updateCustomReminder(reminder: CustomReminderEntity)

    @Delete
    suspend fun deleteCustomReminder(reminder: CustomReminderEntity)

    // 8. ReminderCompletion
    @Query("SELECT * FROM reminder_completions ORDER BY actionTimestampMillis DESC")
    fun observeReminderCompletions(): Flow<List<ReminderCompletionEntity>>

    @Query("SELECT * FROM reminder_completions ORDER BY actionTimestampMillis DESC")
    suspend fun getAllReminderCompletionsOnce(): List<ReminderCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderCompletion(completion: ReminderCompletionEntity): Long

    @Delete
    suspend fun deleteReminderCompletion(completion: ReminderCompletionEntity)

    // 9. NotificationSettings
    @Query("SELECT * FROM notification_settings WHERE id = 1 LIMIT 1")
    fun observeNotificationSettings(): Flow<NotificationSettingsEntity?>

    @Query("SELECT * FROM notification_settings WHERE id = 1 LIMIT 1")
    suspend fun getNotificationSettingsOnce(): NotificationSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotificationSettings(settings: NotificationSettingsEntity)

    // 10. AppSettings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeAppSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppSettings(settings: AppSettingsEntity)

    // Bulk Clear & Restore for Backup/Restore
    @Query("DELETE FROM baby_profiles")
    suspend fun clearAllBabyProfiles()

    @Query("DELETE FROM reminder_completions")
    suspend fun clearAllReminderCompletions()

    @Transaction
    suspend fun restoreFullSnapshot(
        babies: List<BabyProfileEntity>,
        growths: List<GrowthRecordEntity>,
        feedings: List<FeedingRecordEntity>,
        diapers: List<DiaperRecordEntity>,
        hygienes: List<HygieneRecordEntity>,
        cleanings: List<EquipmentCleaningRecordEntity>,
        reminders: List<CustomReminderEntity>,
        completions: List<ReminderCompletionEntity>,
        notificationSettings: NotificationSettingsEntity?,
        appSettings: AppSettingsEntity?
    ) {
        clearAllBabyProfiles()
        clearAllReminderCompletions()
        babies.forEach { insertBabyProfile(it) }
        growths.forEach { insertGrowthRecord(it) }
        feedings.forEach { insertFeedingRecord(it) }
        diapers.forEach { insertDiaperRecord(it) }
        hygienes.forEach { insertHygieneRecord(it) }
        cleanings.forEach { insertEquipmentCleaningRecord(it) }
        reminders.forEach { insertCustomReminder(it) }
        completions.forEach { insertReminderCompletion(it) }
        notificationSettings?.let { upsertNotificationSettings(it) }
        appSettings?.let { upsertAppSettings(it) }
    }
}
