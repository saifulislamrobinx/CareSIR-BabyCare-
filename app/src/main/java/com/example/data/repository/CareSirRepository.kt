package com.example.data.repository

import com.example.data.local.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CareSirBackupPayload(
    val version: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val babies: List<BabyProfileEntity> = emptyList(),
    val growths: List<GrowthRecordEntity> = emptyList(),
    val feedings: List<FeedingRecordEntity> = emptyList(),
    val diapers: List<DiaperRecordEntity> = emptyList(),
    val hygienes: List<HygieneRecordEntity> = emptyList(),
    val cleanings: List<EquipmentCleaningRecordEntity> = emptyList(),
    val reminders: List<CustomReminderEntity> = emptyList(),
    val completions: List<ReminderCompletionEntity> = emptyList(),
    val notificationSettings: NotificationSettingsEntity? = null,
    val appSettings: AppSettingsEntity? = null
)

class CareSirRepository(private val dao: CareSirDao) {

    companion object {
        val DEFAULT_FEEDING_TYPES = listOf(
            "Breastfeeding – Left",
            "Breastfeeding – Right",
            "Breastfeeding – Both",
            "Bottle Feeding",
            "Formula Feeding",
            "Expressed Breast Milk",
            "Other"
        )

        val DEFAULT_DIAPER_TYPES = listOf(
            "Wet",
            "Dirty",
            "Both",
            "Dry"
        )

        val DEFAULT_EQUIPMENT_ACTIVITIES = listOf(
            "Feeding Bottle Cleaning",
            "Feeding Bottle Sterilization",
            "Breast Pump Cleaning",
            "Breast Pump Sterilization",
            "Feeding Accessories Cleaning",
            "Formula Preparation Equipment Cleaning"
        )

        val DEFAULT_HYGIENE_ACTIVITIES = listOf(
            "Bath Time",
            "Face Cleaning",
            "Hand Cleaning",
            "Neck Cleaning",
            "Skin Fold Cleaning",
            "Nail Care",
            "Oral Care",
            "Umbilical Cord Care",
            "Skin Care",
            "Clothing Change",
            "General Hygiene Check"
        )

        val REMINDER_CATEGORIES = listOf(
            "FEEDING",
            "DIAPER",
            "HYGIENE",
            "CLEANING",
            "CUSTOM"
        )

        val REPEAT_TYPES = listOf(
            "NEVER",
            "DAILY",
            "WEEKLY",
            "SELECTED_WEEKDAYS",
            "MONTHLY",
            "CUSTOM_HOURS"
        )
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val allBabies: Flow<List<BabyProfileEntity>> = dao.observeAllBabies()
    val activeBaby: Flow<BabyProfileEntity?> = dao.observeActiveBaby()
    val reminderCompletions: Flow<List<ReminderCompletionEntity>> = dao.observeReminderCompletions()

    val notificationSettings: Flow<NotificationSettingsEntity> =
        dao.observeNotificationSettings().map { it ?: NotificationSettingsEntity() }

    val appSettings: Flow<AppSettingsEntity> =
        dao.observeAppSettings().map { it ?: AppSettingsEntity() }

    fun observeGrowthRecords(babyId: Long): Flow<List<GrowthRecordEntity>> =
        dao.observeGrowthRecords(babyId)

    fun observeFeedingRecords(babyId: Long): Flow<List<FeedingRecordEntity>> =
        dao.observeFeedingRecords(babyId)

    fun observeDiaperRecords(babyId: Long): Flow<List<DiaperRecordEntity>> =
        dao.observeDiaperRecords(babyId)

    fun observeHygieneRecords(babyId: Long): Flow<List<HygieneRecordEntity>> =
        dao.observeHygieneRecords(babyId)

    fun observeEquipmentCleaningRecords(babyId: Long): Flow<List<EquipmentCleaningRecordEntity>> =
        dao.observeEquipmentCleaningRecords(babyId)

    fun observeCustomReminders(babyId: Long): Flow<List<CustomReminderEntity>> =
        dao.observeCustomReminders(babyId)

    suspend fun ensureDefaultsInitialized() {
        if (dao.getNotificationSettingsOnce() == null) {
            dao.upsertNotificationSettings(NotificationSettingsEntity())
        }
        if (dao.getAppSettingsOnce() == null) {
            dao.upsertAppSettings(AppSettingsEntity())
        }
    }

    suspend fun createOrUpdateBabyProfile(
        id: Long = 0,
        fullName: String,
        dateOfBirthMillis: Long,
        gender: String,
        photoUri: String?,
        birthWeightKg: Float?,
        birthHeightCm: Float?,
        currentWeightKg: Float?,
        currentHeightCm: Float?,
        bloodGroup: String = "",
        birthNotes: String = "",
        additionalNotes: String = "",
        completeOnboarding: Boolean = true
    ): Long {
        val now = System.currentTimeMillis()
        val effectiveWeight = currentWeightKg ?: birthWeightKg
        val effectiveHeight = currentHeightCm ?: birthHeightCm

        val entity = BabyProfileEntity(
            id = id,
            fullName = fullName.trim(),
            dateOfBirthMillis = dateOfBirthMillis,
            gender = gender,
            photoUri = photoUri,
            birthWeightKg = birthWeightKg,
            birthHeightCm = birthHeightCm,
            currentWeightKg = effectiveWeight,
            currentHeightCm = effectiveHeight,
            bloodGroup = bloodGroup.trim(),
            birthNotes = birthNotes.trim(),
            additionalNotes = additionalNotes.trim(),
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        val savedId = if (id == 0L) {
            val newId = dao.insertBabyProfile(entity)
            dao.setActiveBaby(newId, now)
            // Record initial growth entry if weight/height provided
            if (effectiveWeight != null || effectiveHeight != null) {
                dao.insertGrowthRecord(
                    GrowthRecordEntity(
                        babyId = newId,
                        weightKg = effectiveWeight ?: 3.2f,
                        heightCm = effectiveHeight ?: 50.0f,
                        notes = "Initial profile measurement",
                        recordedAt = now
                    )
                )
            }
            newId
        } else {
            dao.updateBabyProfile(entity)
            dao.setActiveBaby(id, now)
            id
        }

        if (completeOnboarding) {
            val currentSettings = dao.getAppSettingsOnce() ?: AppSettingsEntity()
            dao.upsertAppSettings(
                currentSettings.copy(
                    onboardingCompleted = true,
                    activeBabyId = savedId,
                    updatedAt = now
                )
            )
        }
        return savedId
    }

    suspend fun switchActiveBaby(babyId: Long) {
        val now = System.currentTimeMillis()
        dao.setActiveBaby(babyId, now)
        val currentSettings = dao.getAppSettingsOnce() ?: AppSettingsEntity()
        dao.upsertAppSettings(currentSettings.copy(activeBabyId = babyId, updatedAt = now))
    }

    suspend fun addGrowthMeasurement(
        babyId: Long,
        weightKg: Float,
        heightCm: Float,
        notes: String,
        recordedAt: Long = System.currentTimeMillis()
    ) {
        val now = System.currentTimeMillis()
        dao.insertGrowthRecord(
            GrowthRecordEntity(
                babyId = babyId,
                weightKg = weightKg,
                heightCm = heightCm,
                notes = notes.trim(),
                recordedAt = recordedAt,
                createdAt = now,
                updatedAt = now
            )
        )
        val active = dao.getActiveBabyOnce()
        if (active != null && active.id == babyId) {
            dao.updateBabyProfile(
                active.copy(
                    currentWeightKg = weightKg,
                    currentHeightCm = heightCm,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun deleteGrowthMeasurement(record: GrowthRecordEntity) {
        dao.deleteGrowthRecord(record)
    }

    suspend fun saveFeedingRecord(
        id: Long = 0,
        babyId: Long,
        feedingType: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        durationSeconds: Long,
        milkAmountMl: Int?,
        breastSide: String,
        notes: String
    ): Pair<Long, Long?> {
        val now = System.currentTimeMillis()
        val notifSettings = dao.getNotificationSettingsOnce() ?: NotificationSettingsEntity()
        val intervalMillis = TimeUnit.MINUTES.toMillis(notifSettings.feedingIntervalMinutes.toLong())
        val referenceTime = maxOf(startTimeMillis, endTimeMillis)
        val nextExpectedMillis = referenceTime + intervalMillis
        val durationMins = ((durationSeconds + 59) / 60).toInt().coerceAtLeast(1)

        val record = FeedingRecordEntity(
            id = id,
            babyId = babyId,
            feedingType = feedingType,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            durationMinutes = durationMins,
            durationSeconds = durationSeconds.coerceAtLeast(1L),
            milkAmountMl = milkAmountMl,
            breastSide = breastSide,
            notes = notes.trim(),
            nextExpectedFeedingMillis = nextExpectedMillis,
            createdAt = now,
            updatedAt = now
        )
        val savedId = if (id == 0L) {
            dao.insertFeedingRecord(record)
        } else {
            dao.updateFeedingRecord(record)
            id
        }

        dao.upsertNotificationSettings(
            notifSettings.copy(
                nextFeedingAlarmMillis = nextExpectedMillis,
                updatedAt = now
            )
        )
        return savedId to (if (notifSettings.feedingNotificationsEnabled) nextExpectedMillis else null)
    }

    suspend fun deleteFeedingRecord(record: FeedingRecordEntity) {
        dao.deleteFeedingRecord(record)
    }

    suspend fun saveDiaperRecord(
        id: Long = 0,
        babyId: Long,
        diaperType: String,
        timestampMillis: Long,
        notes: String
    ): Pair<Long, Long?> {
        val now = System.currentTimeMillis()
        val notifSettings = dao.getNotificationSettingsOnce() ?: NotificationSettingsEntity()
        val nextCheckMillis = timestampMillis + TimeUnit.MINUTES.toMillis(notifSettings.diaperCheckIntervalMinutes.toLong())

        val record = DiaperRecordEntity(
            id = id,
            babyId = babyId,
            diaperType = diaperType,
            timestampMillis = timestampMillis,
            notes = notes.trim(),
            nextCheckReminderMillis = nextCheckMillis,
            createdAt = now,
            updatedAt = now
        )
        val savedId = if (id == 0L) {
            dao.insertDiaperRecord(record)
        } else {
            dao.updateDiaperRecord(record)
            id
        }
        dao.upsertNotificationSettings(
            notifSettings.copy(
                nextDiaperAlarmMillis = nextCheckMillis,
                updatedAt = now
            )
        )
        return savedId to (if (notifSettings.diaperNotificationsEnabled) nextCheckMillis else null)
    }

    suspend fun deleteDiaperRecord(record: DiaperRecordEntity) {
        dao.deleteDiaperRecord(record)
    }

    suspend fun saveEquipmentCleaningRecord(
        id: Long = 0,
        babyId: Long,
        activityName: String,
        completedAtMillis: Long,
        repeatIntervalHours: Int,
        reminderEnabled: Boolean,
        isCustomEquipment: Boolean,
        notes: String
    ): Pair<Long, Long?> {
        val now = System.currentTimeMillis()
        val nextReminderMillis = if (reminderEnabled && repeatIntervalHours > 0) {
            completedAtMillis + TimeUnit.HOURS.toMillis(repeatIntervalHours.toLong())
        } else null

        val record = EquipmentCleaningRecordEntity(
            id = id,
            babyId = babyId,
            activityName = activityName.trim(),
            completedAtMillis = completedAtMillis,
            repeatIntervalHours = repeatIntervalHours.coerceAtLeast(1),
            nextReminderMillis = nextReminderMillis,
            reminderEnabled = reminderEnabled,
            isCustomEquipment = isCustomEquipment,
            notes = notes.trim(),
            createdAt = now,
            updatedAt = now
        )
        val savedId = if (id == 0L) {
            dao.insertEquipmentCleaningRecord(record)
        } else {
            dao.updateEquipmentCleaningRecord(record)
            id
        }

        val notifSettings = dao.getNotificationSettingsOnce() ?: NotificationSettingsEntity()
        if (nextReminderMillis != null) {
            dao.upsertNotificationSettings(
                notifSettings.copy(
                    nextCleaningAlarmMillis = nextReminderMillis,
                    updatedAt = now
                )
            )
        }
        return savedId to (if (notifSettings.cleaningNotificationsEnabled) nextReminderMillis else null)
    }

    suspend fun deleteEquipmentCleaningRecord(record: EquipmentCleaningRecordEntity) {
        dao.deleteEquipmentCleaningRecord(record)
    }

    suspend fun saveHygieneRecord(
        id: Long = 0,
        babyId: Long,
        activityName: String,
        status: String, // COMPLETED or SKIPPED
        timestampMillis: Long,
        notes: String,
        reminderEnabled: Boolean,
        repeatIntervalHours: Int,
        isCustomActivity: Boolean
    ): Pair<Long, Long?> {
        val now = System.currentTimeMillis()
        val nextReminderMillis = if (reminderEnabled && repeatIntervalHours > 0) {
            timestampMillis + TimeUnit.HOURS.toMillis(repeatIntervalHours.toLong())
        } else null

        val record = HygieneRecordEntity(
            id = id,
            babyId = babyId,
            activityName = activityName.trim(),
            status = status,
            timestampMillis = timestampMillis,
            notes = notes.trim(),
            reminderEnabled = reminderEnabled,
            repeatIntervalHours = repeatIntervalHours.coerceAtLeast(1),
            nextReminderMillis = nextReminderMillis,
            isCustomActivity = isCustomActivity,
            createdAt = now,
            updatedAt = now
        )
        val savedId = if (id == 0L) {
            dao.insertHygieneRecord(record)
        } else {
            dao.updateHygieneRecord(record)
            id
        }
        val notifSettings = dao.getNotificationSettingsOnce() ?: NotificationSettingsEntity()
        return savedId to (if (notifSettings.hygieneNotificationsEnabled) nextReminderMillis else null)
    }

    suspend fun deleteHygieneRecord(record: HygieneRecordEntity) {
        dao.deleteHygieneRecord(record)
    }

    suspend fun saveCustomReminder(
        id: Long = 0,
        babyId: Long,
        title: String,
        description: String,
        scheduledTimeMillis: Long,
        repeatType: String,
        selectedWeekdays: String = "",
        customIntervalMinutes: Int = 180,
        category: String = "CUSTOM",
        priority: String = "NORMAL",
        soundTone: String = "GENTLE_CHIME",
        vibrationEnabled: Boolean = true,
        notes: String = "",
        isEnabled: Boolean = true,
        isCompleted: Boolean = false
    ): CustomReminderEntity {
        val now = System.currentTimeMillis()
        val entity = CustomReminderEntity(
            id = id,
            babyId = babyId,
            title = title.trim(),
            description = description.trim(),
            scheduledTimeMillis = scheduledTimeMillis,
            repeatType = repeatType,
            selectedWeekdays = selectedWeekdays,
            customIntervalMinutes = customIntervalMinutes.coerceAtLeast(15),
            category = category,
            priority = priority,
            soundTone = soundTone,
            vibrationEnabled = vibrationEnabled,
            notes = notes.trim(),
            isEnabled = isEnabled,
            isCompleted = isCompleted,
            createdAt = now,
            updatedAt = now
        )
        val finalId = if (id == 0L) {
            dao.insertCustomReminder(entity)
        } else {
            dao.updateCustomReminder(entity)
            id
        }
        return entity.copy(id = finalId)
    }

    suspend fun handleReminderAction(
        reminderId: Long,
        title: String,
        category: String,
        action: String // "COMPLETED", "SNOOZED", "SKIPPED"
    ): CustomReminderEntity? {
        val now = System.currentTimeMillis()
        dao.insertReminderCompletion(
            ReminderCompletionEntity(
                reminderId = reminderId,
                reminderTitle = title,
                category = category,
                actionTaken = action,
                actionTimestampMillis = now,
                createdAt = now,
                updatedAt = now
            )
        )

        val existing = dao.getCustomReminderById(reminderId) ?: return null
        val updated = when (action) {
            "SNOOZED" -> {
                existing.copy(
                    scheduledTimeMillis = now + TimeUnit.MINUTES.toMillis(15),
                    isCompleted = false,
                    lastTriggeredMillis = now,
                    updatedAt = now
                )
            }
            "COMPLETED", "SKIPPED" -> {
                val nextTime = calculateNextRecurringTime(existing, now)
                if (nextTime != null) {
                    existing.copy(
                        scheduledTimeMillis = nextTime,
                        isCompleted = false,
                        lastTriggeredMillis = now,
                        updatedAt = now
                    )
                } else {
                    existing.copy(
                        isCompleted = true,
                        lastTriggeredMillis = now,
                        updatedAt = now
                    )
                }
            }
            else -> existing
        }
        dao.updateCustomReminder(updated)
        return updated
    }

    fun calculateNextRecurringTime(reminder: CustomReminderEntity, referenceNow: Long = System.currentTimeMillis()): Long? {
        val base = maxOf(reminder.scheduledTimeMillis, referenceNow)
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        return when (reminder.repeatType) {
            "DAILY" -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            "WEEKLY" -> {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.timeInMillis
            }
            "SELECTED_WEEKDAYS" -> {
                val allowedDays = reminder.selectedWeekdays.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()
                if (allowedDays.isEmpty()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    cal.timeInMillis
                } else {
                    for (step in 1..7) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
                        if (dow in allowedDays) {
                            return cal.timeInMillis
                        }
                    }
                    cal.timeInMillis
                }
            }
            "MONTHLY" -> {
                cal.add(Calendar.MONTH, 1)
                cal.timeInMillis
            }
            "CUSTOM_HOURS" -> {
                base + TimeUnit.MINUTES.toMillis(reminder.customIntervalMinutes.coerceAtLeast(15).toLong())
            }
            else -> null // "NEVER"
        }
    }

    suspend fun deleteCustomReminder(reminder: CustomReminderEntity) {
        dao.deleteCustomReminder(reminder)
    }

    suspend fun updateNotificationSettings(settings: NotificationSettingsEntity) {
        dao.upsertNotificationSettings(settings.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateAppSettings(settings: AppSettingsEntity) {
        dao.upsertAppSettings(settings.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun getNotificationSettingsOnce(): NotificationSettingsEntity =
        dao.getNotificationSettingsOnce() ?: NotificationSettingsEntity()

    suspend fun getAllActiveCustomRemindersOnce(): List<CustomReminderEntity> =
        dao.getAllCustomRemindersOnce().filter { it.isEnabled && !it.isCompleted }

    // Backup & Restore JSON
    suspend fun exportBackupJson(): String {
        val payload = CareSirBackupPayload(
            version = 1,
            exportedAtMillis = System.currentTimeMillis(),
            babies = dao.getAllBabiesOnce(),
            growths = dao.getAllGrowthRecordsOnce(),
            feedings = dao.getAllFeedingRecordsOnce(),
            diapers = dao.getAllDiaperRecordsOnce(),
            hygienes = dao.getAllHygieneRecordsOnce(),
            cleanings = dao.getAllEquipmentCleaningRecordsOnce(),
            reminders = dao.getAllCustomRemindersOnce(),
            completions = dao.getAllReminderCompletionsOnce(),
            notificationSettings = dao.getNotificationSettingsOnce(),
            appSettings = dao.getAppSettingsOnce()
        )
        val now = System.currentTimeMillis()
        val currentApp = dao.getAppSettingsOnce() ?: AppSettingsEntity()
        dao.upsertAppSettings(currentApp.copy(lastBackupTimestamp = now, updatedAt = now))
        return moshi.adapter(CareSirBackupPayload::class.java).indent("  ").toJson(payload)
    }

    suspend fun importBackupJson(json: String): Boolean {
        return try {
            val adapter = moshi.adapter(CareSirBackupPayload::class.java)
            val payload = adapter.fromJson(json) ?: return false
            dao.restoreFullSnapshot(
                babies = payload.babies,
                growths = payload.growths,
                feedings = payload.feedings,
                diapers = payload.diapers,
                hygienes = payload.hygienes,
                cleanings = payload.cleanings,
                reminders = payload.reminders,
                completions = payload.completions,
                notificationSettings = payload.notificationSettings,
                appSettings = payload.appSettings?.copy(onboardingCompleted = payload.babies.isNotEmpty())
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
