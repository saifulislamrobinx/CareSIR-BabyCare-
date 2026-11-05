package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomReminderEntity
import com.example.data.repository.CareSirRepository
import com.example.ui.components.CareEmptyStateCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.CareSirUiState
import com.example.ui.viewmodel.CareSirViewModel
import com.example.util.DateTimeFormatters
import com.example.util.LocalAppStrings

@Composable
fun RemindersScreen(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    val strings = LocalAppStrings.current
    val now = uiState.currentTimeMillis
    val use24h = uiState.appSettings.use24HourFormat
    val datePattern = uiState.appSettings.dateFormatPattern

    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0=Upcoming, 1=Overdue, 2=Completed, 3=History
    var showReminderForm by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<CustomReminderEntity?>(null) }

    val upcomingReminders = remember(uiState.customReminders, now) {
        uiState.customReminders.filter { !it.isCompleted && it.scheduledTimeMillis >= now - 60_000L }
            .sortedBy { it.scheduledTimeMillis }
    }
    val overdueReminders = remember(uiState.customReminders, now) {
        uiState.customReminders.filter { !it.isCompleted && it.scheduledTimeMillis < now - 60_000L }
            .sortedBy { it.scheduledTimeMillis }
    }
    val completedReminders = remember(uiState.customReminders) {
        uiState.customReminders.filter { it.isCompleted }
            .sortedByDescending { it.updatedAt }
    }

    if (showReminderForm || editingReminder != null) {
        CustomReminderFormDialog(
            initialReminder = editingReminder,
            onDismiss = {
                showReminderForm = false
                editingReminder = null
            },
            onSave = { id, title, desc, scheduledMillis, repeatType, weekdays, customMins, cat, prio, vibrate, notes ->
                viewModel.saveCustomReminder(
                    id = id,
                    title = title,
                    description = desc,
                    scheduledTimeMillis = scheduledMillis,
                    repeatType = repeatType,
                    selectedWeekdays = weekdays,
                    customIntervalMinutes = customMins,
                    category = cat,
                    priority = prio,
                    vibrationEnabled = vibrate,
                    notes = notes
                )
                showReminderForm = false
                editingReminder = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Reminder Engine",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Create unlimited custom routines & recurring schedules",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showReminderForm = true },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("add_custom_reminder_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                ) {
                    Icon(Icons.Filled.AddAlarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addReminder)
                }
            }
        }

        // Test Live Notification Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SageGreenContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, SageGreen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Android Notification Engine Active",
                            style = MaterialTheme.typography.labelLarge,
                            color = DarkCharcoal
                        )
                        Text(
                            text = "5 channels • Done, Snooze 15m & Skip actions • Reboot recovery",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkCharcoal.copy(alpha = 0.8f)
                        )
                    }
                    FilledTonalButton(
                        onClick = { viewModel.triggerImmediateTestNotification("CUSTOM") },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("test_notification_button")
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Alert")
                    }
                }
            }
        }

        // Filter Tabs
        item {
            val tabLabels = listOf(
                "Upcoming (${upcomingReminders.size})",
                "Overdue (${overdueReminders.size})",
                "Completed (${completedReminders.size})",
                "Action Log (${uiState.reminderCompletions.size})"
            )
            ScrollableTabRow(
                selectedTabIndex = selectedFilterTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent
            ) {
                tabLabels.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedFilterTab == idx,
                        onClick = { selectedFilterTab = idx },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        text = { Text(label) }
                    )
                }
            }
        }

        when (selectedFilterTab) {
            0, 1, 2 -> {
                val listToShow = when (selectedFilterTab) {
                    0 -> upcomingReminders
                    1 -> overdueReminders
                    else -> completedReminders
                }

                if (listToShow.isEmpty()) {
                    item {
                        CareEmptyStateCard(
                            icon = Icons.Outlined.Alarm,
                            title = "No reminders in this view",
                            message = "Tap '${strings.addReminder}' to create any custom baby or family care schedule.",
                            actionLabel = strings.addReminder,
                            onActionClick = { showReminderForm = true }
                        )
                    }
                } else {
                    items(listToShow, key = { it.id }) { reminder ->
                        ReminderItemCard(
                            reminder = reminder,
                            nowMillis = now,
                            datePattern = datePattern,
                            use24h = use24h,
                            isBangla = strings.isBangla,
                            onToggleEnabled = { viewModel.toggleCustomReminderEnabled(reminder, it) },
                            onAction = { action -> viewModel.performReminderAction(reminder, action) },
                            onEdit = { editingReminder = reminder },
                            onDelete = { viewModel.deleteCustomReminder(reminder) }
                        )
                    }
                }
            }
            else -> {
                if (uiState.reminderCompletions.isEmpty()) {
                    item {
                        CareEmptyStateCard(
                            icon = Icons.Outlined.History,
                            title = "No reminder actions recorded yet",
                            message = "When you mark a reminder Done, Snooze, or Skip, it will appear here."
                        )
                    }
                } else {
                    items(uiState.reminderCompletions, key = { it.id }) { comp ->
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
                                        text = comp.reminderTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${comp.category} • ${DateTimeFormatters.formatDateTime(comp.actionTimestampMillis, datePattern, use24h)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SageGreenContainer
                                ) {
                                    Text(
                                        text = comp.actionTaken,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SageGreenDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderItemCard(
    reminder: CustomReminderEntity,
    nowMillis: Long,
    datePattern: String,
    use24h: Boolean,
    isBangla: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onAction: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val (countdown, isOverdue) = DateTimeFormatters.formatCountdownStatus(
        targetMillis = reminder.scheduledTimeMillis,
        nowMillis = nowMillis,
        isBangla = isBangla
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isOverdue && !reminder.isCompleted) ErrorMutedRedBg else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isOverdue && !reminder.isCompleted) DarkCharcoal else MaterialTheme.colorScheme.onSurface
                    )
                    if (reminder.description.isNotBlank()) {
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue && !reminder.isCompleted) DarkCharcoal.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${reminder.category} • ${reminder.repeatType} • Priority: ${reminder.priority}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SageGreenDark
                    )
                    Text(
                        text = "${DateTimeFormatters.formatDateTime(reminder.scheduledTimeMillis, datePattern, use24h)} ($countdown)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue && !reminder.isCompleted) ErrorMutedRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!reminder.isCompleted) {
                    FilledTonalButton(
                        onClick = { onAction("COMPLETED") },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(strings.done)
                    }
                    OutlinedButton(
                        onClick = { onAction("SNOOZED") },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(strings.snooze)
                    }
                    TextButton(
                        onClick = { onAction("SKIPPED") },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(strings.skip)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Reminder")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete Reminder", tint = ErrorMutedRed)
                }
            }
        }
    }
}

@Composable
private fun CustomReminderFormDialog(
    initialReminder: CustomReminderEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        description: String,
        scheduledTimeMillis: Long,
        repeatType: String,
        selectedWeekdays: String,
        customIntervalMinutes: Int,
        category: String,
        priority: String,
        vibrationEnabled: Boolean,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var description by remember { mutableStateOf(initialReminder?.description ?: "") }
    var offsetMinutesText by remember { mutableStateOf("60") }
    var repeatType by remember { mutableStateOf(initialReminder?.repeatType ?: "DAILY") }
    var selectedWeekdays by remember { mutableStateOf(initialReminder?.selectedWeekdays ?: "2,3,4,5,6") }
    var customIntervalText by remember { mutableStateOf((initialReminder?.customIntervalMinutes ?: 180).toString()) }
    var category by remember { mutableStateOf(initialReminder?.category ?: "CUSTOM") }
    var priority by remember { mutableStateOf(initialReminder?.priority ?: "NORMAL") }
    var vibrationEnabled by remember { mutableStateOf(initialReminder?.vibrationEnabled ?: true) }
    var notes by remember { mutableStateOf(initialReminder?.notes ?: "") }
    var titleError by remember { mutableStateOf(false) }

    val quickIdeas = listOf(
        "Morning sunlight routine",
        "Close doors and windows before Maghrib Azan",
        "Prepare feeding bottle",
        "Check room temperature",
        "Change baby clothes",
        "Doctor appointment"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialReminder == null) "New Smart Reminder" else "Edit Smart Reminder") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Quick Idea Templates (Tap to fill or type your own):", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickIdeas.forEach { idea ->
                        SuggestionChip(
                            onClick = {
                                title = idea
                                titleError = false
                            },
                            label = { Text(idea, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("Reminder Title *") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = offsetMinutesText,
                        onValueChange = { offsetMinutesText = it },
                        label = { Text("Remind in (mins)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customIntervalText,
                        onValueChange = { customIntervalText = it },
                        label = { Text("Repeat mins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Repeat Schedule", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CareSirRepository.REPEAT_TYPES.forEach { rType ->
                        FilterChip(
                            selected = repeatType == rType,
                            onClick = { repeatType = rType },
                            label = { Text(rType, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Category & Priority", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CareSirRepository.REMINDER_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HIGH", "NORMAL", "LOW").forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        val minsFromNow = offsetMinutesText.toLongOrNull() ?: 60L
                        val scheduledMillis = initialReminder?.scheduledTimeMillis
                            ?: (System.currentTimeMillis() + minsFromNow * 60_000L)
                        val customMins = customIntervalText.toIntOrNull()?.coerceAtLeast(15) ?: 180
                        onSave(
                            initialReminder?.id ?: 0L,
                            title.trim(),
                            description.trim(),
                            scheduledMillis,
                            repeatType,
                            selectedWeekdays,
                            customMins,
                            category,
                            priority,
                            vibrationEnabled,
                            notes
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_save_reminder_button")
            ) {
                Text("Save Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
