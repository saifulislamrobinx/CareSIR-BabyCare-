package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.CustomReminderEntity
import com.example.data.repository.CareSirRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CareSirUiState
import com.example.util.DateTimeFormatters
import com.example.util.LocalAppStrings
import java.util.Calendar

@Composable
fun DashboardScreen(
    uiState: CareSirUiState,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenFeedingTab: () -> Unit,
    onOpenDiaperTab: () -> Unit,
    onOpenCleaningTab: () -> Unit,
    onOpenHygieneTab: () -> Unit,
    onOpenRemindersScreen: () -> Unit,
    onStartFeedingTimer: (String) -> Unit,
    onPauseResumeTimer: () -> Unit,
    onStopAndSaveTimer: () -> Unit,
    onQuickLogFeeding: () -> Unit,
    onQuickLogDiaper: (String) -> Unit,
    onQuickCompleteHygiene: (String) -> Unit,
    onQuickCleanEquipment: (String) -> Unit,
    onReminderAction: (CustomReminderEntity, String) -> Unit,
    onCategoryAlarmAction: (String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val now = uiState.currentTimeMillis
    val startOfToday = DateTimeFormatters.startOfDayMillis(now)
    val use24h = uiState.appSettings.use24HourFormat
    val datePattern = uiState.appSettings.dateFormatPattern

    val hour = remember(now) {
        Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    }
    val greeting = when {
        hour < 12 -> strings.greetingMorning
        hour < 17 -> strings.greetingAfternoon
        else -> strings.greetingEvening
    }

    val baby = uiState.activeBaby
    val todayFeedings = remember(uiState.feedingRecords, startOfToday) {
        uiState.feedingRecords.filter { it.startTimeMillis >= startOfToday }
    }
    val lastFeeding = uiState.feedingRecords.firstOrNull()
    val nextFeedingMillis = uiState.notificationSettings.nextFeedingAlarmMillis
        ?: lastFeeding?.nextExpectedFeedingMillis

    val todayDiapers = remember(uiState.diaperRecords, startOfToday) {
        uiState.diaperRecords.filter { it.timestampMillis >= startOfToday }
    }
    val lastDiaper = uiState.diaperRecords.firstOrNull()
    val nextDiaperMillis = uiState.notificationSettings.nextDiaperAlarmMillis
        ?: lastDiaper?.nextCheckReminderMillis

    val todayHygieneCompleted = remember(uiState.hygieneRecords, startOfToday) {
        uiState.hygieneRecords.filter { it.timestampMillis >= startOfToday && it.status == "COMPLETED" }
    }
    val totalDefaultHygieneCount = CareSirRepository.DEFAULT_HYGIENE_ACTIVITIES.size
    val completedHygieneNamesToday = remember(todayHygieneCompleted) {
        todayHygieneCompleted.map { it.activityName }.toSet()
    }
    val pendingHygieneCount = (totalDefaultHygieneCount - completedHygieneNamesToday.size).coerceAtLeast(0)

    val lastCleaning = uiState.cleaningRecords.firstOrNull()
    val nextCleaningMillis = uiState.notificationSettings.nextCleaningAlarmMillis
        ?: lastCleaning?.nextReminderMillis

    val activeReminders = remember(uiState.customReminders) {
        uiState.customReminders.filter { it.isEnabled && !it.isCompleted }
            .sortedBy { it.scheduledTimeMillis }
    }

    // Calculate Daily Care Progress
    val dailyTargetPoints = 8 // e.g., 3+ feedings, 2+ diapers, 2+ hygiene, 1+ cleaning
    val achievedPoints = (todayFeedings.size.coerceAtMost(3) +
            todayDiapers.size.coerceAtMost(2) +
            todayHygieneCompleted.size.coerceAtMost(2) +
            uiState.cleaningRecords.count { it.completedAtMillis >= startOfToday }.coerceAtMost(1))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_lazy_column"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Dashboard Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = SageGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ChildCare,
                                contentDescription = "CareSIR Logo",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SoftPeachLight
                            ) {
                                Text(
                                    text = DateTimeFormatters.formatDate(now, datePattern),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SoftPeachDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$greeting, Parent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dashboard_notification_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeReminders.isNotEmpty()) {
                                    Badge { Text("${activeReminders.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications & Reminders"
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SageGreenContainer)
                            .clickable(onClick = onOpenProfile)
                            .testTag("dashboard_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (baby?.photoUri != null) {
                            AsyncImage(
                                model = baby.photoUri,
                                contentDescription = "Baby Profile Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (baby?.fullName?.firstOrNull()?.uppercase() ?: "B"),
                                style = MaterialTheme.typography.titleMedium,
                                color = SageGreenDark
                            )
                        }
                    }
                }
            }
        }

        // 2. Baby Summary Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProfile)
                    .testTag("baby_summary_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_nursery_banner),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.25f
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(SageGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (baby?.photoUri != null) {
                                        AsyncImage(
                                            model = baby.photoUri,
                                            contentDescription = baby.fullName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Face,
                                            contentDescription = null,
                                            tint = SageGreenDark,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = baby?.fullName ?: "Little One",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = DateTimeFormatters.formatAgeFromDob(
                                            dobMillis = baby?.dateOfBirthMillis ?: now,
                                            nowMillis = now,
                                            isBangla = strings.isBangla
                                        ) + " • ${baby?.gender ?: "Baby"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            AssistChip(
                                onClick = onOpenProfile,
                                label = { Text(strings.navProfile) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricPill(
                                label = "Weight",
                                value = baby?.currentWeightKg?.let { "${it} kg" } ?: "-- kg",
                                accentColor = SageGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "Height",
                                value = baby?.currentHeightCm?.let { "${it} cm" } ?: "-- cm",
                                accentColor = CategoryDiaperColor,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "Feedings Today",
                                value = "${todayFeedings.size}",
                                accentColor = SoftPeachDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Live Feeding Timer Banner if active
        if (uiState.feedingTimer.isRunning) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = SageGreen
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIVE FEEDING SESSION (${uiState.feedingTimer.activeBreastSide})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = DateTimeFormatters.formatDurationSeconds(uiState.feedingTimer.elapsedSeconds),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onPauseResumeTimer,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(if (uiState.feedingTimer.isPaused) strings.resume else strings.pause)
                            }
                            Button(
                                onClick = onStopAndSaveTimer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = SageGreenDark
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("dashboard_stop_save_timer_button")
                            ) {
                                Text(strings.stopAndSave)
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Action Buttons (5 required quick actions)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.quickActionsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionChipButton(
                        label = strings.logFeeding,
                        icon = Icons.Filled.Restaurant,
                        accentColor = CategoryFeedingColor,
                        testTag = "quick_action_feeding",
                        onClick = onQuickLogFeeding
                    )
                    QuickActionChipButton(
                        label = strings.logDiaper,
                        icon = Icons.Filled.BabyChangingStation,
                        accentColor = CategoryDiaperColor,
                        testTag = "quick_action_diaper",
                        onClick = { onQuickLogDiaper("Wet") }
                    )
                    QuickActionChipButton(
                        label = strings.completeHygiene,
                        icon = Icons.Filled.Spa,
                        accentColor = CategoryHygieneColor,
                        testTag = "quick_action_hygiene",
                        onClick = { onQuickCompleteHygiene("Bath Time") }
                    )
                    QuickActionChipButton(
                        label = strings.cleanEquipment,
                        icon = Icons.Filled.CleaningServices,
                        accentColor = CategoryCleaningColor,
                        testTag = "quick_action_cleaning",
                        onClick = { onQuickCleanEquipment("Feeding Bottle Cleaning") }
                    )
                    QuickActionChipButton(
                        label = strings.addReminder,
                        icon = Icons.Filled.AlarmAdd,
                        accentColor = CategoryReminderColor,
                        testTag = "quick_action_reminder",
                        onClick = onOpenRemindersScreen
                    )
                }
            }
        }

        // 4. Daily Care Progress Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DailyProgressRing(
                        completed = achievedPoints,
                        total = dailyTargetPoints
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = strings.dailyProgressTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${todayFeedings.size} feedings • ${todayDiapers.size} diapers • ${todayHygieneCompleted.size} hygiene routines completed today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { (achievedPoints.toFloat() / dailyTargetPoints.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .height(6.dp)
                                .clip(CircleShape),
                            color = SageGreen,
                            trackColor = SageGreen.copy(alpha = 0.16f)
                        )
                    }
                }
            }
        }

        // 5. Feeding Summary Card
        item {
            val (nextFeedCountdown, isFeedOverdue) = DateTimeFormatters.formatCountdownStatus(
                targetMillis = nextFeedingMillis,
                nowMillis = now,
                isBangla = strings.isBangla
            )
            CareSectionCard(
                title = strings.feedingTitle,
                subtitle = if (lastFeeding != null) {
                    "${lastFeeding.feedingType} • ${DateTimeFormatters.formatRelativeTime(lastFeeding.endTimeMillis, now, strings.isBangla)}"
                } else "No feeding logged yet today",
                icon = Icons.Filled.Restaurant,
                accentColor = CategoryFeedingColor,
                trailingAction = {
                    TextButton(onClick = onOpenFeedingTab) {
                        Text("Details")
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = strings.lastFeeding,
                        value = DateTimeFormatters.formatTime(lastFeeding?.endTimeMillis, use24h),
                        accentColor = CategoryFeedingColor,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = strings.nextFeeding,
                        value = DateTimeFormatters.formatTime(nextFeedingMillis, use24h),
                        accentColor = CategoryFeedingColor,
                        isAlert = isFeedOverdue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Countdown",
                        value = nextFeedCountdown,
                        accentColor = CategoryFeedingColor,
                        isAlert = isFeedOverdue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onQuickLogFeeding,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("dashboard_log_feeding_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CategoryFeedingColor)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.logFeeding)
                    }
                    if (!uiState.feedingTimer.isRunning) {
                        OutlinedButton(
                            onClick = { onStartFeedingTimer("Left") },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("dashboard_start_timer_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Timer")
                        }
                    }
                }
            }
        }

        // 6. Diaper Summary Card
        item {
            val (diaperCountdown, isDiaperOverdue) = DateTimeFormatters.formatCountdownStatus(
                targetMillis = nextDiaperMillis,
                nowMillis = now,
                isBangla = strings.isBangla
            )
            CareSectionCard(
                title = strings.diaperTitle,
                subtitle = "${strings.todayCount}: ${todayDiapers.size} changes",
                icon = Icons.Filled.BabyChangingStation,
                accentColor = CategoryDiaperColor,
                trailingAction = {
                    TextButton(onClick = onOpenDiaperTab) {
                        Text("History")
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = strings.lastDiaper,
                        value = if (lastDiaper != null) {
                            "${lastDiaper.diaperType} (${DateTimeFormatters.formatTime(lastDiaper.timestampMillis, use24h)})"
                        } else "--",
                        accentColor = CategoryDiaperColor,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = strings.nextDiaperCheck,
                        value = diaperCountdown,
                        accentColor = CategoryDiaperColor,
                        isAlert = isDiaperOverdue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CareSirRepository.DEFAULT_DIAPER_TYPES.forEach { dType ->
                        FilledTonalButton(
                            onClick = { onQuickLogDiaper(dType) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("dashboard_quick_diaper_${dType.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Text(dType, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 7. Hygiene Summary Card & 8. Equipment Cleaning Card
        item {
            CareSectionCard(
                title = strings.hygieneTitle,
                subtitle = "${todayHygieneCompleted.size} completed • $pendingHygieneCount pending today",
                icon = Icons.Filled.Spa,
                accentColor = CategoryHygieneColor,
                trailingAction = {
                    TextButton(onClick = onOpenHygieneTab) {
                        Text("View All")
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = strings.completedTasks,
                        value = "${todayHygieneCompleted.size}",
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = strings.pendingTasks,
                        value = "$pendingHygieneCount",
                        accentColor = CategoryHygieneColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            val (cleanCountdown, isCleanOverdue) = DateTimeFormatters.formatCountdownStatus(
                targetMillis = nextCleaningMillis,
                nowMillis = now,
                isBangla = strings.isBangla
            )
            CareSectionCard(
                title = strings.cleaningTitle,
                subtitle = lastCleaning?.activityName ?: "Keep bottles & pump accessories sterilized",
                icon = Icons.Filled.CleaningServices,
                accentColor = CategoryCleaningColor,
                trailingAction = {
                    TextButton(onClick = onOpenCleaningTab) {
                        Text("Manage")
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = strings.lastCleaned,
                        value = DateTimeFormatters.formatTime(lastCleaning?.completedAtMillis, use24h),
                        accentColor = CategoryCleaningColor,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = strings.nextCleaning,
                        value = cleanCountdown,
                        accentColor = CategoryCleaningColor,
                        isAlert = isCleanOverdue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 9. Upcoming & Overdue Reminders Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.upcomingRemindersTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onOpenRemindersScreen) {
                    Text("See All (${activeReminders.size})")
                }
            }
        }

        if (activeReminders.isEmpty()) {
            item {
                CareEmptyStateCard(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "No custom reminders scheduled",
                    message = "Add custom reminders for doctor visits, vitamin drops, sunlight routine, or family care.",
                    actionLabel = strings.addReminder,
                    onActionClick = onOpenRemindersScreen
                )
            }
        } else {
            items(activeReminders.take(4), key = { it.id }) { reminder ->
                val (countdownLabel, isOverdue) = DateTimeFormatters.formatCountdownStatus(
                    targetMillis = reminder.scheduledTimeMillis,
                    nowMillis = now,
                    isBangla = strings.isBangla
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isOverdue) ErrorMutedRedBg else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isOverdue) ErrorMutedRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOverdue) DarkCharcoal else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${reminder.category} • ${reminder.priority} • ${
                                        DateTimeFormatters.formatDateTime(
                                            reminder.scheduledTimeMillis,
                                            datePattern,
                                            use24h
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOverdue) ErrorMutedRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isOverdue) ErrorMutedRed else SageGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = countdownLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOverdue) Color.White else SageGreenDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onReminderAction(reminder, "COMPLETED") },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.done, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { onReminderAction(reminder, "SNOOZED") },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(strings.snooze, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(
                                onClick = { onReminderAction(reminder, "SKIPPED") },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(strings.skip)
                            }
                        }
                    }
                }
            }
        }
    }
}
