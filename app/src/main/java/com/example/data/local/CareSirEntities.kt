package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "baby_profiles")
data class BabyProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val dateOfBirthMillis: Long,
    val gender: String, // "Boy", "Girl", "Other"
    val photoUri: String? = null,
    val birthWeightKg: Float? = null,
    val birthHeightCm: Float? = null,
    val currentWeightKg: Float? = null,
    val currentHeightCm: Float? = null,
    val bloodGroup: String = "",
    val birthNotes: String = "",
    val additionalNotes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "growth_records",
    foreignKeys = [
        ForeignKey(
            entity = BabyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("babyId"), Index("recordedAt")]
)
data class GrowthRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val weightKg: Float,
    val heightCm: Float,
    val headCircumferenceCm: Float? = null,
    val notes: String = "",
    val recordedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "feeding_records",
    foreignKeys = [
        ForeignKey(
            entity = BabyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("babyId"), Index("startTimeMillis")]
)
data class FeedingRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val feedingType: String, // Breastfeeding – Left, Breastfeeding – Right, Breastfeeding – Both, Bottle Feeding, Formula Feeding, Expressed Breast Milk, Other
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMinutes: Int,
    val durationSeconds: Long = durationMinutes * 60L,
    val milkAmountMl: Int? = null,
    val breastSide: String = "", // Left, Right, Both, N/A
    val notes: String = "",
    val nextExpectedFeedingMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "diaper_records",
    foreignKeys = [
        ForeignKey(
            entity = BabyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("babyId"), Index("timestampMillis")]
)
data class DiaperRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val diaperType: String, // Wet, Dirty, Both, Dry
    val timestampMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val nextCheckReminderMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "hygiene_records",
    foreignKeys = [
        ForeignKey(
            entity = BabyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("babyId"), Index("timestampMillis")]
)
data class HygieneRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val activityName: String, // Bath Time, Face Cleaning, Hand Cleaning, Neck Cleaning, Skin Fold Cleaning, Nail Care, Oral Care, Umbilical Cord Care, Skin Care, Clothing Change, General Hygiene Check, or Custom
    val status: String = "COMPLETED", // COMPLETED, SKIPPED, PENDING
    val timestampMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val reminderEnabled: Boolean = false,
    val repeatIntervalHours: Int = 24,
    val nextReminderMillis: Long? = null,
    val isCustomActivity: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "equipment_cleaning_records",
    foreignKeys = [
        ForeignKey(
            entity = BabyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("babyId"), Index("completedAtMillis")]
)
data class EquipmentCleaningRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val activityName: String, // Feeding Bottle Cleaning, Feeding Bottle Sterilization, Breast Pump Cleaning, Breast Pump Sterilization, Feeding Accessories Cleaning, Formula Preparation Equipment Cleaning, or Custom
    val completedAtMillis: Long = System.currentTimeMillis(),
    val repeatIntervalHours: Int = 4,
    val nextReminderMillis: Long? = null,
    val reminderEnabled: Boolean = true,
    val isCustomEquipment: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "custom_reminders",
    foreignKeys = [
        ForeignKey(
            entity = BabyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("babyId"), Index("scheduledTimeMillis")]
)
data class CustomReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val title: String,
    val description: String = "",
    val scheduledTimeMillis: Long,
    val repeatType: String = "DAILY", // NEVER, DAILY, WEEKLY, SELECTED_WEEKDAYS, MONTHLY, CUSTOM_HOURS
    val selectedWeekdays: String = "", // e.g., "1,2,3,4,5" (Mon-Fri)
    val customIntervalMinutes: Int = 180,
    val category: String = "CUSTOM", // FEEDING, DIAPER, HYGIENE, CLEANING, CUSTOM
    val priority: String = "NORMAL", // HIGH, NORMAL, LOW
    val soundTone: String = "GENTLE_CHIME",
    val vibrationEnabled: Boolean = true,
    val notes: String = "",
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val lastTriggeredMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "reminder_completions",
    indices = [Index("reminderId"), Index("actionTimestampMillis")]
)
data class ReminderCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val reminderTitle: String,
    val category: String,
    val actionTaken: String, // COMPLETED, SNOOZED, SKIPPED
    val actionTimestampMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val feedingNotificationsEnabled: Boolean = true,
    val feedingIntervalMinutes: Int = 150, // default 2.5 hours
    val diaperNotificationsEnabled: Boolean = true,
    val diaperCheckIntervalMinutes: Int = 180, // default 3 hours
    val hygieneNotificationsEnabled: Boolean = true,
    val cleaningNotificationsEnabled: Boolean = true,
    val cleaningIntervalMinutes: Int = 240, // default 4 hours
    val customNotificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22, // 10:00 PM
    val quietHoursEndHour: Int = 6,   // 6:00 AM
    val nextFeedingAlarmMillis: Long? = null,
    val nextDiaperAlarmMillis: Long? = null,
    val nextCleaningAlarmMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingCompleted: Boolean = false,
    val activeBabyId: Long? = null,
    val languageCode: String = "en", // "en" or "bn"
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val use24HourFormat: Boolean = false,
    val dateFormatPattern: String = "dd MMM yyyy",
    val lastBackupTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
