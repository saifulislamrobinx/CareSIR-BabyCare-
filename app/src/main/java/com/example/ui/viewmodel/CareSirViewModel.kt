package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.CareSirRepository
import com.example.notifications.CareNotificationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CareSirViewModel(
    private val repository: CareSirRepository,
    private val appContext: Context
) : ViewModel() {

    private val _timerState = MutableStateFlow(FeedingTimerState())
    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    private val _selectedActivityTab = MutableStateFlow(0)
    private val _historyCategoryFilter = MutableStateFlow(UnifiedActivityCategory.ALL)
    private val _historyDateFilter = MutableStateFlow(DateRangeFilter.LAST_7_DAYS)
    private val _customDateRange = MutableStateFlow(
        (System.currentTimeMillis() - 7 * 86_400_000L) to System.currentTimeMillis()
    )
    private val _statusBanner = MutableStateFlow<String?>(null)

    private val activeBabyFlow: StateFlow<BabyProfileEntity?> = repository.activeBaby
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val babyRecordsFlow = activeBabyFlow.flatMapLatest { baby ->
        if (baby == null) {
            flowOf(
                BabyScopedData(
                    growths = emptyList(),
                    feedings = emptyList(),
                    diapers = emptyList(),
                    hygienes = emptyList(),
                    cleanings = emptyList(),
                    reminders = emptyList()
                )
            )
        } else {
            combine(
                repository.observeGrowthRecords(baby.id),
                repository.observeFeedingRecords(baby.id),
                repository.observeDiaperRecords(baby.id),
                repository.observeHygieneRecords(baby.id),
                repository.observeEquipmentCleaningRecords(baby.id),
                repository.observeCustomReminders(baby.id)
            ) { arr ->
                @Suppress("UNCHECKED_CAST")
                BabyScopedData(
                    growths = arr[0] as List<GrowthRecordEntity>,
                    feedings = arr[1] as List<FeedingRecordEntity>,
                    diapers = arr[2] as List<DiaperRecordEntity>,
                    hygienes = arr[3] as List<HygieneRecordEntity>,
                    cleanings = arr[4] as List<EquipmentCleaningRecordEntity>,
                    reminders = arr[5] as List<CustomReminderEntity>
                )
            }
        }
    }

    val uiState: StateFlow<CareSirUiState> = combine(
        repository.appSettings,
        repository.notificationSettings,
        repository.allBabies,
        activeBabyFlow,
        babyRecordsFlow,
        repository.reminderCompletions,
        _timerState,
        _nowMillis,
        _selectedActivityTab,
        _historyCategoryFilter,
        _historyDateFilter,
        _customDateRange,
        _statusBanner
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val appSettings = values[0] as AppSettingsEntity
        val notifSettings = values[1] as NotificationSettingsEntity
        val allBabies = values[2] as List<BabyProfileEntity>
        val activeBaby = values[3] as BabyProfileEntity?
        val scoped = values[4] as BabyScopedData
        val completions = values[5] as List<ReminderCompletionEntity>
        val timer = values[6] as FeedingTimerState
        val now = values[7] as Long
        val actTab = values[8] as Int
        val catFilter = values[9] as UnifiedActivityCategory
        val dateFilter = values[10] as DateRangeFilter
        val customRange = values[11] as Pair<Long, Long>
        val banner = values[12] as String?

        CareSirUiState(
            isLoading = false,
            appSettings = appSettings,
            notificationSettings = notifSettings,
            allBabies = allBabies,
            activeBaby = activeBaby,
            growthRecords = scoped.growths,
            feedingRecords = scoped.feedings,
            diaperRecords = scoped.diapers,
            hygieneRecords = scoped.hygienes,
            cleaningRecords = scoped.cleanings,
            customReminders = scoped.reminders,
            reminderCompletions = completions,
            feedingTimer = timer,
            currentTimeMillis = now,
            selectedActivityTab = actTab,
            historyCategoryFilter = catFilter,
            historyDateFilter = dateFilter,
            customStartDateMillis = customRange.first,
            customEndDateMillis = customRange.second,
            statusBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CareSirUiState()
    )

    init {
        CareNotificationManager.createNotificationChannels(appContext)
        viewModelScope.launch {
            repository.ensureDefaultsInitialized()
        }
        // Ticker for live feeding timer & dynamic countdowns
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _nowMillis.value = System.currentTimeMillis()
                val currentTimer = _timerState.value
                if (currentTimer.isRunning && !currentTimer.isPaused) {
                    _timerState.value = currentTimer.copy(
                        elapsedSeconds = currentTimer.elapsedSeconds + 1L
                    )
                }
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun selectActivityTab(index: Int) {
        _selectedActivityTab.value = index.coerceIn(0, 3)
    }

    fun setHistoryFilters(
        category: UnifiedActivityCategory = _historyCategoryFilter.value,
        dateFilter: DateRangeFilter = _historyDateFilter.value,
        customStart: Long = _customDateRange.value.first,
        customEnd: Long = _customDateRange.value.second
    ) {
        _historyCategoryFilter.value = category
        _historyDateFilter.value = dateFilter
        _customDateRange.value = customStart to customEnd
    }

    // Baby Profile & Onboarding
    fun saveBabyProfile(
        id: Long = 0L,
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
        additionalNotes: String = ""
    ) {
        if (fullName.isBlank()) return
        viewModelScope.launch {
            repository.createOrUpdateBabyProfile(
                id = id,
                fullName = fullName,
                dateOfBirthMillis = dateOfBirthMillis,
                gender = gender,
                photoUri = photoUri,
                birthWeightKg = birthWeightKg,
                birthHeightCm = birthHeightCm,
                currentWeightKg = currentWeightKg,
                currentHeightCm = currentHeightCm,
                bloodGroup = bloodGroup,
                birthNotes = birthNotes,
                additionalNotes = additionalNotes,
                completeOnboarding = true
            )
            showBanner("Baby profile saved for ${fullName.trim()}")
        }
    }

    fun switchBabyProfile(babyId: Long) {
        viewModelScope.launch {
            repository.switchActiveBaby(babyId)
        }
    }

    fun addGrowthMeasurement(weightKg: Float, heightCm: Float, notes: String, recordedAt: Long = System.currentTimeMillis()) {
        val babyId = uiState.value.activeBaby?.id ?: return
        viewModelScope.launch {
            repository.addGrowthMeasurement(babyId, weightKg, heightCm, notes, recordedAt)
            showBanner("Growth measurement recorded (${weightKg} kg, ${heightCm} cm)")
        }
    }

    fun deleteGrowthMeasurement(record: GrowthRecordEntity) {
        viewModelScope.launch {
            repository.deleteGrowthMeasurement(record)
        }
    }

    // Live Feeding Timer
    fun startFeedingTimer(breastSide: String = "Left") {
        val now = System.currentTimeMillis()
        _timerState.value = FeedingTimerState(
            isRunning = true,
            isPaused = false,
            startTimeMillis = now,
            elapsedSeconds = 0L,
            activeBreastSide = breastSide,
            feedingType = "Breastfeeding – $breastSide"
        )
    }

    fun pauseOrResumeFeedingTimer() {
        val cur = _timerState.value
        if (!cur.isRunning) return
        _timerState.value = cur.copy(isPaused = !cur.isPaused)
    }

    fun switchTimerBreastSide(side: String) {
        val cur = _timerState.value
        val updatedSide = if (cur.activeBreastSide != side && cur.elapsedSeconds > 10) "Both" else side
        _timerState.value = cur.copy(
            activeBreastSide = side,
            feedingType = "Breastfeeding – $updatedSide"
        )
    }

    fun cancelFeedingTimer() {
        _timerState.value = FeedingTimerState()
    }

    fun stopAndSaveFeedingTimer(notes: String = "") {
        val cur = _timerState.value
        if (!cur.isRunning) return
        val babyId = uiState.value.activeBaby?.id ?: return
        val endMillis = System.currentTimeMillis()
        val durationSecs = cur.elapsedSeconds.coerceAtLeast(1L)
        val startMillis = if (cur.startTimeMillis > 0L) cur.startTimeMillis else (endMillis - durationSecs * 1000L)
        _timerState.value = FeedingTimerState()

        saveFeedingRecord(
            id = 0L,
            feedingType = cur.feedingType,
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            durationSeconds = durationSecs,
            milkAmountMl = null,
            breastSide = cur.activeBreastSide,
            notes = notes
        )
    }

    fun saveFeedingRecord(
        id: Long = 0L,
        feedingType: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        durationSeconds: Long,
        milkAmountMl: Int?,
        breastSide: String,
        notes: String
    ) {
        val babyId = uiState.value.activeBaby?.id ?: return
        viewModelScope.launch {
            val (_, nextExpectedMillis) = repository.saveFeedingRecord(
                id = id,
                babyId = babyId,
                feedingType = feedingType,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                durationSeconds = durationSeconds,
                milkAmountMl = milkAmountMl,
                breastSide = breastSide,
                notes = notes
            )
            if (nextExpectedMillis != null) {
                CareNotificationManager.scheduleFeedingReminder(appContext, nextExpectedMillis)
            }
            showBanner("Feeding logged • Next reminder updated")
        }
    }

    fun deleteFeedingRecord(record: FeedingRecordEntity) {
        viewModelScope.launch {
            repository.deleteFeedingRecord(record)
            showBanner("Feeding record deleted")
        }
    }

    fun updateFeedingIntervalMinutes(intervalMinutes: Int, enabled: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.notificationSettings
            val latestFeeding = uiState.value.feedingRecords.firstOrNull()
            val baseTime = latestFeeding?.endTimeMillis ?: System.currentTimeMillis()
            val nextTime = baseTime + intervalMinutes * 60_000L
            val updated = current.copy(
                feedingIntervalMinutes = intervalMinutes,
                feedingNotificationsEnabled = enabled,
                nextFeedingAlarmMillis = nextTime
            )
            repository.updateNotificationSettings(updated)
            if (enabled) {
                CareNotificationManager.scheduleFeedingReminder(appContext, nextTime)
            }
            showBanner("Feeding reminder interval set to ${intervalMinutes}m")
        }
    }

    fun snoozeOrSkipCategoryAlarm(category: String, action: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val settings = uiState.value.notificationSettings
            when (category.uppercase()) {
                "FEEDING" -> {
                    val nextTime = if (action == "SNOOZE") {
                        now + 15 * 60_000L
                    } else {
                        now + settings.feedingIntervalMinutes * 60_000L
                    }
                    repository.updateNotificationSettings(settings.copy(nextFeedingAlarmMillis = nextTime))
                    if (settings.feedingNotificationsEnabled) {
                        CareNotificationManager.scheduleFeedingReminder(appContext, nextTime)
                    }
                }
                "DIAPER" -> {
                    val nextTime = if (action == "SNOOZE") {
                        now + 15 * 60_000L
                    } else {
                        now + settings.diaperCheckIntervalMinutes * 60_000L
                    }
                    repository.updateNotificationSettings(settings.copy(nextDiaperAlarmMillis = nextTime))
                    if (settings.diaperNotificationsEnabled) {
                        CareNotificationManager.scheduleDiaperReminder(appContext, nextTime)
                    }
                }
            }
            showBanner("$category reminder ${action.lowercase()}d")
        }
    }

    // Diaper Tracker
    fun saveDiaperRecord(
        id: Long = 0L,
        diaperType: String,
        timestampMillis: Long = System.currentTimeMillis(),
        notes: String = ""
    ) {
        val babyId = uiState.value.activeBaby?.id ?: return
        viewModelScope.launch {
            val (_, nextCheckMillis) = repository.saveDiaperRecord(
                id = id,
                babyId = babyId,
                diaperType = diaperType,
                timestampMillis = timestampMillis,
                notes = notes
            )
            if (nextCheckMillis != null) {
                CareNotificationManager.scheduleDiaperReminder(appContext, nextCheckMillis)
            }
            showBanner("Diaper change ($diaperType) recorded")
        }
    }

    fun deleteDiaperRecord(record: DiaperRecordEntity) {
        viewModelScope.launch {
            repository.deleteDiaperRecord(record)
            showBanner("Diaper record deleted")
        }
    }

    fun updateDiaperIntervalMinutes(intervalMinutes: Int, enabled: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.notificationSettings
            val latestDiaper = uiState.value.diaperRecords.firstOrNull()
            val baseTime = latestDiaper?.timestampMillis ?: System.currentTimeMillis()
            val nextTime = baseTime + intervalMinutes * 60_000L
            repository.updateNotificationSettings(
                current.copy(
                    diaperCheckIntervalMinutes = intervalMinutes,
                    diaperNotificationsEnabled = enabled,
                    nextDiaperAlarmMillis = nextTime
                )
            )
            if (enabled) {
                CareNotificationManager.scheduleDiaperReminder(appContext, nextTime)
            }
            showBanner("Diaper check interval updated")
        }
    }

    // Equipment Cleaning & Sterilization Tracker
    fun saveEquipmentCleaningRecord(
        id: Long = 0L,
        activityName: String,
        completedAtMillis: Long = System.currentTimeMillis(),
        repeatIntervalHours: Int = 4,
        reminderEnabled: Boolean = true,
        isCustomEquipment: Boolean = false,
        notes: String = ""
    ) {
        val babyId = uiState.value.activeBaby?.id ?: return
        viewModelScope.launch {
            val (_, nextReminderMillis) = repository.saveEquipmentCleaningRecord(
                id = id,
                babyId = babyId,
                activityName = activityName,
                completedAtMillis = completedAtMillis,
                repeatIntervalHours = repeatIntervalHours,
                reminderEnabled = reminderEnabled,
                isCustomEquipment = isCustomEquipment,
                notes = notes
            )
            if (nextReminderMillis != null) {
                CareNotificationManager.scheduleCleaningReminder(appContext, activityName, nextReminderMillis)
            }
            showBanner("$activityName logged")
        }
    }

    fun deleteEquipmentCleaningRecord(record: EquipmentCleaningRecordEntity) {
        viewModelScope.launch {
            repository.deleteEquipmentCleaningRecord(record)
            showBanner("Equipment cleaning record deleted")
        }
    }

    // Baby Personal Hygiene Tracker
    fun saveHygieneRecord(
        id: Long = 0L,
        activityName: String,
        status: String = "COMPLETED",
        timestampMillis: Long = System.currentTimeMillis(),
        notes: String = "",
        reminderEnabled: Boolean = true,
        repeatIntervalHours: Int = 24,
        isCustomActivity: Boolean = false
    ) {
        val babyId = uiState.value.activeBaby?.id ?: return
        viewModelScope.launch {
            val (_, nextReminderMillis) = repository.saveHygieneRecord(
                id = id,
                babyId = babyId,
                activityName = activityName,
                status = status,
                timestampMillis = timestampMillis,
                notes = notes,
                reminderEnabled = reminderEnabled,
                repeatIntervalHours = repeatIntervalHours,
                isCustomActivity = isCustomActivity
            )
            if (nextReminderMillis != null) {
                CareNotificationManager.scheduleHygieneReminder(appContext, activityName, nextReminderMillis)
            }
            showBanner("Hygiene activity ($activityName) marked $status")
        }
    }

    fun deleteHygieneRecord(record: HygieneRecordEntity) {
        viewModelScope.launch {
            repository.deleteHygieneRecord(record)
            showBanner("Hygiene record deleted")
        }
    }

    // Custom Smart Reminder Engine
    fun saveCustomReminder(
        id: Long = 0L,
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
        isEnabled: Boolean = true
    ) {
        val babyId = uiState.value.activeBaby?.id ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            val saved = repository.saveCustomReminder(
                id = id,
                babyId = babyId,
                title = title,
                description = description,
                scheduledTimeMillis = scheduledTimeMillis,
                repeatType = repeatType,
                selectedWeekdays = selectedWeekdays,
                customIntervalMinutes = customIntervalMinutes,
                category = category,
                priority = priority,
                soundTone = soundTone,
                vibrationEnabled = vibrationEnabled,
                notes = notes,
                isEnabled = isEnabled
            )
            CareNotificationManager.scheduleCustomReminder(appContext, saved)
            showBanner("Reminder '${saved.title}' saved")
        }
    }

    fun toggleCustomReminderEnabled(reminder: CustomReminderEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updated = repository.saveCustomReminder(
                id = reminder.id,
                babyId = reminder.babyId,
                title = reminder.title,
                description = reminder.description,
                scheduledTimeMillis = reminder.scheduledTimeMillis,
                repeatType = reminder.repeatType,
                selectedWeekdays = reminder.selectedWeekdays,
                customIntervalMinutes = reminder.customIntervalMinutes,
                category = reminder.category,
                priority = reminder.priority,
                soundTone = reminder.soundTone,
                vibrationEnabled = reminder.vibrationEnabled,
                notes = reminder.notes,
                isEnabled = enabled,
                isCompleted = reminder.isCompleted
            )
            if (enabled) {
                CareNotificationManager.scheduleCustomReminder(appContext, updated)
            } else {
                CareNotificationManager.cancelCustomReminder(appContext, updated.id)
            }
        }
    }

    fun performReminderAction(reminder: CustomReminderEntity, action: String) {
        viewModelScope.launch {
            val updated = repository.handleReminderAction(
                reminderId = reminder.id,
                title = reminder.title,
                category = reminder.category,
                action = action
            )
            if (updated != null) {
                if (updated.isEnabled && !updated.isCompleted) {
                    CareNotificationManager.scheduleCustomReminder(appContext, updated)
                } else {
                    CareNotificationManager.cancelCustomReminder(appContext, updated.id)
                }
            }
            showBanner("Reminder '${reminder.title}' marked ${action.lowercase()}")
        }
    }

    fun deleteCustomReminder(reminder: CustomReminderEntity) {
        viewModelScope.launch {
            CareNotificationManager.cancelCustomReminder(appContext, reminder.id)
            repository.deleteCustomReminder(reminder)
            showBanner("Reminder deleted")
        }
    }

    fun triggerImmediateTestNotification(category: String = "CUSTOM") {
        val settings = uiState.value.notificationSettings
        CareNotificationManager.showReminderNotification(
            context = appContext,
            notificationId = 8888 + category.hashCode() % 100,
            reminderId = -10L,
            title = "CareSIR Smart Alert ($category)",
            message = "Your $category notification channel is active with Done, Snooze 15m, and Skip actions.",
            category = category,
            priority = "HIGH",
            settings = settings
        )
        showBanner("Test notification sent to $category channel")
    }

    // Settings & Preferences
    fun updateAppSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch {
            val updated = transform(uiState.value.appSettings)
            repository.updateAppSettings(updated)
        }
    }

    fun updateNotificationSettings(transform: (NotificationSettingsEntity) -> NotificationSettingsEntity) {
        viewModelScope.launch {
            val updated = transform(uiState.value.notificationSettings)
            repository.updateNotificationSettings(updated)
        }
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importBackupJson(json)
            onResult(success)
            showBanner(if (success) "Backup restored successfully!" else "Invalid backup JSON format")
        }
    }

    private data class BabyScopedData(
        val growths: List<GrowthRecordEntity>,
        val feedings: List<FeedingRecordEntity>,
        val diapers: List<DiaperRecordEntity>,
        val hygienes: List<HygieneRecordEntity>,
        val cleanings: List<EquipmentCleaningRecordEntity>,
        val reminders: List<CustomReminderEntity>
    )

    class Factory(
        private val repository: CareSirRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CareSirViewModel(repository, appContext) as T
        }
    }
}
