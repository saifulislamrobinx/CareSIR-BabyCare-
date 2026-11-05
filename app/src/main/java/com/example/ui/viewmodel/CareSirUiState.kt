package com.example.ui.viewmodel

import com.example.data.local.*

data class FeedingTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val startTimeMillis: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val activeBreastSide: String = "Left", // "Left", "Right", "Both"
    val feedingType: String = "Breastfeeding – Left"
)

enum class UnifiedActivityCategory {
    ALL, FEEDING, DIAPER, CLEANING, HYGIENE, REMINDERS
}

enum class DateRangeFilter {
    TODAY, YESTERDAY, LAST_7_DAYS, LAST_30_DAYS, CUSTOM
}

data class UnifiedActivityItem(
    val uniqueKey: String,
    val entityId: Long,
    val category: UnifiedActivityCategory,
    val title: String,
    val subtitle: String,
    val details: String,
    val statusLabel: String,
    val timestampMillis: Long
)

data class CareSirUiState(
    val isLoading: Boolean = true,
    val appSettings: AppSettingsEntity = AppSettingsEntity(),
    val notificationSettings: NotificationSettingsEntity = NotificationSettingsEntity(),
    val allBabies: List<BabyProfileEntity> = emptyList(),
    val activeBaby: BabyProfileEntity? = null,
    val growthRecords: List<GrowthRecordEntity> = emptyList(),
    val feedingRecords: List<FeedingRecordEntity> = emptyList(),
    val diaperRecords: List<DiaperRecordEntity> = emptyList(),
    val hygieneRecords: List<HygieneRecordEntity> = emptyList(),
    val cleaningRecords: List<EquipmentCleaningRecordEntity> = emptyList(),
    val customReminders: List<CustomReminderEntity> = emptyList(),
    val reminderCompletions: List<ReminderCompletionEntity> = emptyList(),
    val feedingTimer: FeedingTimerState = FeedingTimerState(),
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val selectedActivityTab: Int = 0, // 0=Feeding, 1=Diaper, 2=Cleaning, 3=Hygiene
    val historyCategoryFilter: UnifiedActivityCategory = UnifiedActivityCategory.ALL,
    val historyDateFilter: DateRangeFilter = DateRangeFilter.LAST_7_DAYS,
    val customStartDateMillis: Long = System.currentTimeMillis() - 7 * 86_400_000L,
    val customEndDateMillis: Long = System.currentTimeMillis(),
    val statusBannerMessage: String? = null
)
