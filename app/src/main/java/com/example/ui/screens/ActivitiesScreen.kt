package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.repository.CareSirRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CareSirUiState
import com.example.ui.viewmodel.CareSirViewModel
import com.example.util.DateTimeFormatters
import com.example.util.LocalAppStrings

@Composable
fun ActivitiesScreen(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    val strings = LocalAppStrings.current
    val tabs = listOf(
        strings.feedingTitle to Icons.Filled.Restaurant,
        strings.diaperTitle to Icons.Filled.BabyChangingStation,
        strings.cleaningTitle to Icons.Filled.CleaningServices,
        strings.hygieneTitle to Icons.Filled.Spa
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = uiState.selectedActivityTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { idx, (label, icon) ->
                Tab(
                    selected = uiState.selectedActivityTab == idx,
                    onClick = { viewModel.selectActivityTab(idx) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 52.dp)
                        .testTag("activity_tab_$idx"),
                    text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        when (uiState.selectedActivityTab) {
            0 -> FeedingTrackerModule(uiState = uiState, viewModel = viewModel)
            1 -> DiaperTrackerModule(uiState = uiState, viewModel = viewModel)
            2 -> EquipmentCleaningModule(uiState = uiState, viewModel = viewModel)
            else -> BabyHygieneModule(uiState = uiState, viewModel = viewModel)
        }
    }
}

@Composable
private fun FeedingTrackerModule(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    val strings = LocalAppStrings.current
    val now = uiState.currentTimeMillis
    val startOfToday = DateTimeFormatters.startOfDayMillis(now)
    val use24h = uiState.appSettings.use24HourFormat
    val datePattern = uiState.appSettings.dateFormatPattern

    val todayFeedings = remember(uiState.feedingRecords, startOfToday) {
        uiState.feedingRecords.filter { it.startTimeMillis >= startOfToday }
    }
    val todayBreastMinutes = remember(todayFeedings) {
        todayFeedings.filter { it.feedingType.contains("Breastfeeding") }.sumOf { it.durationMinutes }
    }
    val todayBottleMl = remember(todayFeedings) {
        todayFeedings.sumOf { it.milkAmountMl ?: 0 }
    }

    var showManualDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<FeedingRecordEntity?>(null) }
    var customIntervalText by remember { mutableStateOf(uiState.notificationSettings.feedingIntervalMinutes.toString()) }

    if (showManualDialog || editingRecord != null) {
        FeedingEntryDialog(
            initialRecord = editingRecord,
            onDismiss = {
                showManualDialog = false
                editingRecord = null
            },
            onSave = { id, type, durationMins, amountMl, side, notes ->
                val endMillis = editingRecord?.endTimeMillis ?: System.currentTimeMillis()
                val startMillis = endMillis - durationMins * 60_000L
                viewModel.saveFeedingRecord(
                    id = id,
                    feedingType = type,
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis,
                    durationSeconds = durationMins * 60L,
                    milkAmountMl = amountMl,
                    breastSide = side,
                    notes = notes
                )
                showManualDialog = false
                editingRecord = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Medical Safety Note
        item {
            SafetyGuidanceBanner(text = strings.medicalFeedingDisclaimer)
        }

        // One-tap Live Feeding Timer Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (uiState.feedingTimer.isRunning) SageGreen else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (uiState.feedingTimer.isRunning) {
                            "FEEDING TIMER • ${uiState.feedingTimer.activeBreastSide.uppercase()} BREAST"
                        } else "ONE-TAP LIVE FEEDING TIMER",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.feedingTimer.isRunning) Color.White else SageGreenDark
                    )

                    Text(
                        text = DateTimeFormatters.formatDurationSeconds(uiState.feedingTimer.elapsedSeconds),
                        style = MaterialTheme.typography.displayLarge,
                        color = if (uiState.feedingTimer.isRunning) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    // Left / Right breast switcher
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Left", "Right", "Both").forEach { side ->
                            val selected = uiState.feedingTimer.activeBreastSide == side
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (uiState.feedingTimer.isRunning) {
                                        viewModel.switchTimerBreastSide(side)
                                    } else {
                                        viewModel.startFeedingTimer(side)
                                    }
                                },
                                label = { Text("$side Breast") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (uiState.feedingTimer.isRunning) Color.White else SageGreenContainer,
                                    selectedLabelColor = SageGreenDark
                                )
                            )
                        }
                    }

                    if (!uiState.feedingTimer.isRunning) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startFeedingTimer("Left") },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 50.dp)
                                    .testTag("start_feeding_timer_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.startFeedingTimer)
                            }
                            OutlinedButton(
                                onClick = { showManualDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 50.dp)
                                    .testTag("open_manual_feeding_dialog_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.EditNote, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manual Entry")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.pauseOrResumeFeedingTimer() },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 50.dp)
                                    .testTag("pause_resume_timer_button")
                            ) {
                                Text(if (uiState.feedingTimer.isPaused) strings.resume else strings.pause)
                            }
                            Button(
                                onClick = { viewModel.stopAndSaveFeedingTimer() },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 50.dp)
                                    .testTag("stop_save_timer_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = SageGreenDark
                                )
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.stopAndSave)
                            }
                        }
                    }
                }
            }
        }

        // Feeding Dashboard Stats
        item {
            val lastFeed = uiState.feedingRecords.firstOrNull()
            val nextFeedMillis = uiState.notificationSettings.nextFeedingAlarmMillis ?: lastFeed?.nextExpectedFeedingMillis
            val (countdown, isOverdue) = DateTimeFormatters.formatCountdownStatus(nextFeedMillis, now, strings.isBangla)

            CareSectionCard(
                title = "Today's Feeding Summary",
                subtitle = "Last session: ${DateTimeFormatters.formatRelativeTime(lastFeed?.endTimeMillis, now, strings.isBangla)}",
                icon = Icons.Outlined.Analytics,
                accentColor = CategoryFeedingColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = "Sessions Today",
                        value = "${todayFeedings.size}",
                        accentColor = CategoryFeedingColor,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Breastfeeding",
                        value = "${todayBreastMinutes}m",
                        accentColor = CategoryFeedingColor,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Bottle / Formula",
                        value = "${todayBottleMl} ml",
                        accentColor = SoftPeachDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricPill(
                        label = "Next Expected",
                        value = "${DateTimeFormatters.formatTime(nextFeedMillis, use24h)} ($countdown)",
                        accentColor = CategoryFeedingColor,
                        isAlert = isOverdue,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.snoozeOrSkipCategoryAlarm("FEEDING", "SNOOZE") },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(strings.snooze)
                    }
                    TextButton(
                        onClick = { viewModel.snoozeOrSkipCategoryAlarm("FEEDING", "SKIP") },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(strings.skip)
                    }
                }
            }
        }

        // Configurable Feeding Interval Reminder System
        item {
            CareSectionCard(
                title = "Feeding Reminder Interval",
                subtitle = "Automatically recalculates after every logged feeding",
                icon = Icons.Outlined.Schedule,
                accentColor = SageGreen,
                trailingAction = {
                    Switch(
                        checked = uiState.notificationSettings.feedingNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateFeedingIntervalMinutes(
                                uiState.notificationSettings.feedingIntervalMinutes,
                                enabled
                            )
                        }
                    )
                }
            ) {
                val intervals = listOf(
                    "1h" to 60,
                    "1.5h" to 90,
                    "2h" to 120,
                    "2.5h" to 150,
                    "3h" to 180
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intervals.forEach { (label, mins) ->
                        val selected = uiState.notificationSettings.feedingIntervalMinutes == mins
                        FilterChip(
                            selected = selected,
                            onClick = {
                                customIntervalText = mins.toString()
                                viewModel.updateFeedingIntervalMinutes(
                                    mins,
                                    uiState.notificationSettings.feedingNotificationsEnabled
                                )
                            },
                            label = { Text("Every $label") },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customIntervalText,
                        onValueChange = { customIntervalText = it },
                        label = { Text("Custom Interval (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilledTonalButton(
                        onClick = {
                            val mins = customIntervalText.toIntOrNull()?.coerceIn(15, 720) ?: 150
                            viewModel.updateFeedingIntervalMinutes(
                                mins,
                                uiState.notificationSettings.feedingNotificationsEnabled
                            )
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }

        // Feeding History
        item {
            Text(
                text = "Feeding History (${uiState.feedingRecords.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (uiState.feedingRecords.isEmpty()) {
            item {
                CareEmptyStateCard(
                    icon = Icons.Outlined.Restaurant,
                    title = "No feeding sessions logged yet",
                    message = "Tap Start Feeding Timer or Manual Entry above to record your baby's first feeding.",
                    actionLabel = "Log Feeding Now",
                    onActionClick = { showManualDialog = true }
                )
            }
        } else {
            items(uiState.feedingRecords, key = { it.id }) { record ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.feedingType,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${DateTimeFormatters.formatDateTime(record.startTimeMillis, datePattern, use24h)} • ${record.durationMinutes} min" +
                                        (record.milkAmountMl?.let { " • $it ml" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (record.notes.isNotBlank()) {
                                Text(
                                    text = record.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SageGreenDark
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { editingRecord = record }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit Feeding")
                            }
                            IconButton(onClick = { viewModel.deleteFeedingRecord(record) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete Feeding",
                                    tint = ErrorMutedRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedingEntryDialog(
    initialRecord: FeedingRecordEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, Int, Int?, String, String) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(initialRecord?.feedingType ?: CareSirRepository.DEFAULT_FEEDING_TYPES.first())
    }
    var durationText by remember { mutableStateOf((initialRecord?.durationMinutes ?: 15).toString()) }
    var milkAmountText by remember { mutableStateOf(initialRecord?.milkAmountMl?.toString() ?: "90") }
    var breastSide by remember { mutableStateOf(initialRecord?.breastSide?.ifBlank { "Left" } ?: "Left") }
    var notes by remember { mutableStateOf(initialRecord?.notes ?: "") }

    val needsVolume = selectedType.contains("Bottle") ||
            selectedType.contains("Formula") ||
            selectedType.contains("Expressed")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRecord == null) "Log Feeding Session" else "Edit Feeding Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Feeding Category", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CareSirRepository.DEFAULT_FEEDING_TYPES.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { cat ->
                                FilterChip(
                                    selected = selectedType == cat,
                                    onClick = {
                                        selectedType = cat
                                        breastSide = when {
                                            cat.contains("Left") -> "Left"
                                            cat.contains("Right") -> "Right"
                                            cat.contains("Both") -> "Both"
                                            else -> "N/A"
                                        }
                                    },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duration (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (needsVolume) {
                        OutlinedTextField(
                            value = milkAmountText,
                            onValueChange = { milkAmountText = it },
                            label = { Text("Amount (ml)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dur = durationText.toIntOrNull()?.coerceAtLeast(1) ?: 15
                    val ml = if (needsVolume) milkAmountText.toIntOrNull() else null
                    onSave(initialRecord?.id ?: 0L, selectedType, dur, ml, breastSide, notes)
                },
                modifier = Modifier.testTag("confirm_save_feeding_button")
            ) {
                Text("Save Feeding")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DiaperTrackerModule(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    val now = uiState.currentTimeMillis
    val startOfToday = DateTimeFormatters.startOfDayMillis(now)
    val use24h = uiState.appSettings.use24HourFormat
    val datePattern = uiState.appSettings.dateFormatPattern

    var selectedType by remember { mutableStateOf("Wet") }
    var notesText by remember { mutableStateOf("") }
    var editingDiaper by remember { mutableStateOf<DiaperRecordEntity?>(null) }

    val todayDiapers = remember(uiState.diaperRecords, startOfToday) {
        uiState.diaperRecords.filter { it.timestampMillis >= startOfToday }
    }

    if (editingDiaper != null) {
        val current = editingDiaper!!
        var editType by remember { mutableStateOf(current.diaperType) }
        var editNotes by remember { mutableStateOf(current.notes) }
        AlertDialog(
            onDismissRequest = { editingDiaper = null },
            title = { Text("Edit Diaper Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CareSirRepository.DEFAULT_DIAPER_TYPES.forEach { t ->
                            FilterChip(
                                selected = editType == t,
                                onClick = { editType = t },
                                label = { Text(t) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveDiaperRecord(current.id, editType, current.timestampMillis, editNotes)
                    editingDiaper = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingDiaper = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CareSectionCard(
                title = "Log Diaper Change",
                subtitle = "Today's changes: ${todayDiapers.size}",
                icon = Icons.Filled.BabyChangingStation,
                accentColor = CategoryDiaperColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CareSirRepository.DEFAULT_DIAPER_TYPES.forEach { dType ->
                        FilterChip(
                            selected = selectedType == dType,
                            onClick = { selectedType = dType },
                            label = { Text(dType) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Optional Notes (rash check, color, consistency)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Button(
                    onClick = {
                        viewModel.saveDiaperRecord(
                            id = 0L,
                            diaperType = selectedType,
                            timestampMillis = System.currentTimeMillis(),
                            notes = notesText
                        )
                        notesText = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .testTag("save_diaper_record_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CategoryDiaperColor)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Diaper Change ($selectedType)")
                }
            }
        }

        // Diaper Check Reminder Schedule
        item {
            CareSectionCard(
                title = "Recurring Diaper Check Reminder",
                subtitle = "Every ${uiState.notificationSettings.diaperCheckIntervalMinutes / 60f} hours",
                icon = Icons.Outlined.NotificationsActive,
                accentColor = CategoryDiaperColor,
                trailingAction = {
                    Switch(
                        checked = uiState.notificationSettings.diaperNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateDiaperIntervalMinutes(
                                uiState.notificationSettings.diaperCheckIntervalMinutes,
                                enabled
                            )
                        }
                    )
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("2h" to 120, "3h" to 180, "4h" to 240).forEach { (label, mins) ->
                        FilterChip(
                            selected = uiState.notificationSettings.diaperCheckIntervalMinutes == mins,
                            onClick = {
                                viewModel.updateDiaperIntervalMinutes(
                                    mins,
                                    uiState.notificationSettings.diaperNotificationsEnabled
                                )
                            },
                            label = { Text("Every $label") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Text("Diaper History (${uiState.diaperRecords.size})", style = MaterialTheme.typography.titleMedium)
        }

        items(uiState.diaperRecords, key = { it.id }) { record ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Diaper: ${record.diaperType}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = DateTimeFormatters.formatDateTime(record.timestampMillis, datePattern, use24h),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (record.notes.isNotBlank()) {
                            Text(record.notes, style = MaterialTheme.typography.bodySmall, color = CategoryDiaperColor)
                        }
                    }
                    Row {
                        IconButton(onClick = { editingDiaper = record }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit Diaper")
                        }
                        IconButton(onClick = { viewModel.deleteDiaperRecord(record) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete Diaper", tint = ErrorMutedRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentCleaningModule(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    val strings = LocalAppStrings.current
    val use24h = uiState.appSettings.use24HourFormat
    val datePattern = uiState.appSettings.dateFormatPattern

    var selectedActivity by remember { mutableStateOf(CareSirRepository.DEFAULT_EQUIPMENT_ACTIVITIES.first()) }
    var customActivityName by remember { mutableStateOf("") }
    var repeatHours by remember { mutableIntStateOf(4) }
    var notes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SafetyGuidanceBanner(text = strings.cleaningSafetyDisclaimer)
        }

        item {
            CareSectionCard(
                title = "Equipment Cleaning & Sterilization",
                subtitle = "Log bottle, breast pump, and formula equipment care",
                icon = Icons.Filled.CleaningServices,
                accentColor = CategoryCleaningColor
            ) {
                CareSirRepository.DEFAULT_EQUIPMENT_ACTIVITIES.forEach { activity ->
                    val latest = uiState.cleaningRecords.firstOrNull { it.activityName == activity }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(activity, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = if (latest != null) {
                                        "Last: ${DateTimeFormatters.formatDateTime(latest.completedAtMillis, datePattern, use24h)}"
                                    } else "Not logged yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    viewModel.saveEquipmentCleaningRecord(
                                        activityName = activity,
                                        repeatIntervalHours = repeatHours,
                                        notes = notes
                                    )
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Clean")
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Add Custom Equipment Cleaning Activity", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customActivityName,
                        onValueChange = { customActivityName = it },
                        label = { Text("Custom Equipment Name") },
                        placeholder = { Text("e.g., Pacifier & Teether Sterilization") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            val name = customActivityName.trim().ifBlank { selectedActivity }
                            viewModel.saveEquipmentCleaningRecord(
                                activityName = name,
                                repeatIntervalHours = repeatHours,
                                isCustomEquipment = customActivityName.isNotBlank(),
                                notes = notes
                            )
                            customActivityName = ""
                            notes = ""
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("save_custom_cleaning_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CategoryCleaningColor)
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        item {
            Text("Cleaning History (${uiState.cleaningRecords.size})", style = MaterialTheme.typography.titleMedium)
        }

        items(uiState.cleaningRecords, key = { it.id }) { record ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.activityName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "${DateTimeFormatters.formatDateTime(record.completedAtMillis, datePattern, use24h)} • Repeat every ${record.repeatIntervalHours}h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.deleteEquipmentCleaningRecord(record) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Cleaning Record", tint = ErrorMutedRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun BabyHygieneModule(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    val strings = LocalAppStrings.current
    val now = uiState.currentTimeMillis
    val startOfToday = DateTimeFormatters.startOfDayMillis(now)
    val use24h = uiState.appSettings.use24HourFormat
    val datePattern = uiState.appSettings.dateFormatPattern

    var customHygieneName by remember { mutableStateOf("") }
    var customNotes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SafetyGuidanceBanner(text = strings.hygieneSafetyDisclaimer)
        }

        item {
            CareSectionCard(
                title = "Daily Baby Personal Hygiene Checklist",
                subtitle = "Mark each activity Completed or Skipped for today's routine",
                icon = Icons.Filled.Spa,
                accentColor = CategoryHygieneColor
            ) {
                CareSirRepository.DEFAULT_HYGIENE_ACTIVITIES.forEach { activity ->
                    val todayRecord = uiState.hygieneRecords.firstOrNull {
                        it.activityName == activity && it.timestampMillis >= startOfToday
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = when (todayRecord?.status) {
                            "COMPLETED" -> SuccessGreenBg
                            "SKIPPED" -> WarningAmberBg
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activity,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (todayRecord != null) DarkCharcoal else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (todayRecord != null) {
                                        "${todayRecord.status} at ${DateTimeFormatters.formatTime(todayRecord.timestampMillis, use24h)}"
                                    } else "Pending today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (todayRecord != null) DarkCharcoal.copy(alpha = 0.75f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.saveHygieneRecord(
                                            activityName = activity,
                                            status = "COMPLETED"
                                        )
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text(strings.done)
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.saveHygieneRecord(
                                            activityName = activity,
                                            status = "SKIPPED"
                                        )
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text(strings.skip)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Add Custom Hygiene Activity", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customHygieneName,
                        onValueChange = { customHygieneName = it },
                        label = { Text("Custom Activity") },
                        placeholder = { Text("e.g., Gentle Hair Brushing") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            if (customHygieneName.isNotBlank()) {
                                viewModel.saveHygieneRecord(
                                    activityName = customHygieneName.trim(),
                                    status = "COMPLETED",
                                    notes = customNotes,
                                    isCustomActivity = true
                                )
                                customHygieneName = ""
                                customNotes = ""
                            }
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("save_custom_hygiene_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CategoryHygieneColor)
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        item {
            Text("Hygiene History (${uiState.hygieneRecords.size})", style = MaterialTheme.typography.titleMedium)
        }

        items(uiState.hygieneRecords, key = { it.id }) { record ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${record.activityName} • ${record.status}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = DateTimeFormatters.formatDateTime(record.timestampMillis, datePattern, use24h),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.deleteHygieneRecord(record) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Hygiene Record", tint = ErrorMutedRed)
                    }
                }
            }
        }
    }
}
