package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.CareSirRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import com.example.util.DateTimeFormatters
import com.example.util.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsAndHistoryScreen(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    var topMode by remember { mutableIntStateOf(0) } // 0 = Reports & Analytics, 1 = Unified Activity History

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = topMode,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = topMode == 0,
                onClick = { topMode = 0 },
                modifier = Modifier
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag("reports_tab_analytics"),
                text = { Text("Reports & Analytics") },
                icon = { Icon(Icons.Outlined.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = topMode == 1,
                onClick = { topMode = 1 },
                modifier = Modifier
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag("reports_tab_history"),
                text = { Text("Activity History") },
                icon = { Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (topMode == 0) {
            ReportsAnalyticsSection(uiState = uiState)
        } else {
            UnifiedActivityHistorySection(uiState = uiState, viewModel = viewModel)
        }
    }
}

@Composable
private fun ReportsAnalyticsSection(uiState: CareSirUiState) {
    val strings = LocalAppStrings.current
    val now = uiState.currentTimeMillis
    val startOfToday = DateTimeFormatters.startOfDayMillis(now)
    var reportPeriod by remember { mutableIntStateOf(0) } // 0 = Daily, 1 = Weekly, 2 = Monthly

    // Daily metrics
    val todayFeedings = uiState.feedingRecords.filter { it.startTimeMillis >= startOfToday }
    val todayFeedingMins = todayFeedings.sumOf { it.durationMinutes }
    val todayBottleMl = todayFeedings.sumOf { it.milkAmountMl ?: 0 }
    val todayDiapers = uiState.diaperRecords.count { it.timestampMillis >= startOfToday }
    val todayHygieneDone = uiState.hygieneRecords.count { it.timestampMillis >= startOfToday && it.status == "COMPLETED" }
    val todayCleaningDone = uiState.cleaningRecords.count { it.completedAtMillis >= startOfToday }
    val todayRemindersDone = uiState.reminderCompletions.count {
        it.actionTimestampMillis >= startOfToday && it.actionTaken == "COMPLETED"
    }

    // 7-day daily feeding & diaper series
    val dayBuckets = remember(now, uiState.feedingRecords, uiState.diaperRecords) {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        (6 downTo 0).map { daysAgo ->
            val dayStart = DateTimeFormatters.startOfDayMillis(now - daysAgo * 86_400_000L)
            val dayEnd = DateTimeFormatters.endOfDayMillis(now - daysAgo * 86_400_000L)
            val label = dayFormat.format(Date(dayStart))
            val feedCount = uiState.feedingRecords.count { it.startTimeMillis in dayStart..dayEnd }
            val diaperCount = uiState.diaperRecords.count { it.timestampMillis in dayStart..dayEnd }
            Triple(label, feedCount, diaperCount)
        }
    }

    // 30-day metrics
    val thirtyDaysAgo = now - 30 * 86_400_000L
    val monthFeedings = uiState.feedingRecords.filter { it.startTimeMillis >= thirtyDaysAgo }
    val monthDiapers = uiState.diaperRecords.filter { it.timestampMillis >= thirtyDaysAgo }
    val monthHygiene = uiState.hygieneRecords.filter { it.timestampMillis >= thirtyDaysAgo && it.status == "COMPLETED" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SafetyGuidanceBanner(text = strings.reportsDisclaimer)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Daily Report", "Weekly Report", "Monthly Report").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = reportPeriod == idx,
                        onClick = { reportPeriod = idx },
                        label = { Text(label) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    )
                }
            }
        }

        if (reportPeriod == 0) {
            // Daily Report
            item {
                CareSectionCard(
                    title = "Daily Care Report (${DateTimeFormatters.formatDate(now, uiState.appSettings.dateFormatPattern)})",
                    subtitle = "Complete summary of today's logged activities",
                    icon = Icons.Filled.Today,
                    accentColor = SageGreen
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            label = "Feeding Sessions",
                            value = "${todayFeedings.size}",
                            accentColor = CategoryFeedingColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Feeding Duration",
                            value = "${todayFeedingMins}m",
                            accentColor = CategoryFeedingColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Bottle Volume",
                            value = "${todayBottleMl} ml",
                            accentColor = SoftPeachDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            label = "Diaper Changes",
                            value = "$todayDiapers",
                            accentColor = CategoryDiaperColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Hygiene Done",
                            value = "$todayHygieneDone",
                            accentColor = CategoryHygieneColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            label = "Equipment Cleaned",
                            value = "$todayCleaningDone",
                            accentColor = CategoryCleaningColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Reminders Done",
                            value = "$todayRemindersDone",
                            accentColor = CategoryReminderColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else if (reportPeriod == 1) {
            // Weekly Report
            item {
                val weekFeedTotal = dayBuckets.sumOf { it.second }
                val weekDiaperTotal = dayBuckets.sumOf { it.third }
                val sevenDaysAgo = now - 7 * 86_400_000L
                val weekHygieneDone = uiState.hygieneRecords.count {
                    it.timestampMillis >= sevenDaysAgo && it.status == "COMPLETED"
                }
                val hygieneCompletionPct = ((weekHygieneDone.toFloat() / (CareSirRepository.DEFAULT_HYGIENE_ACTIVITIES.size * 7f)) * 100f)
                    .toInt().coerceIn(0, 100)

                CareSectionCard(
                    title = "Weekly Care Analytics (Last 7 Days)",
                    subtitle = "Configured feeding interval: ${uiState.notificationSettings.feedingIntervalMinutes}m",
                    icon = Icons.Filled.DateRange,
                    accentColor = CategoryFeedingColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            label = "7d Feedings",
                            value = "$weekFeedTotal",
                            accentColor = CategoryFeedingColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "7d Diapers",
                            value = "$weekDiaperTotal",
                            accentColor = CategoryDiaperColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Hygiene Routine",
                            value = "$hygieneCompletionPct%",
                            accentColor = CategoryHygieneColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Daily Feeding (Green) vs Diaper (Blue) Count",
                        style = MaterialTheme.typography.labelLarge
                    )
                    WeeklyDualBarChart(
                        buckets = dayBuckets,
                        primaryColor = CategoryFeedingColor,
                        secondaryColor = CategoryDiaperColor
                    )
                }
            }
        } else {
            // Monthly Report
            item {
                val latestGrowth = uiState.growthRecords.firstOrNull()
                val earliestGrowth = uiState.growthRecords.lastOrNull()
                CareSectionCard(
                    title = "Monthly Trends & Growth (30 Days)",
                    subtitle = "30-day overview of feeding, diapers, hygiene, and baby growth",
                    icon = Icons.Filled.AutoGraph,
                    accentColor = CategoryHygieneColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            label = "30d Feedings",
                            value = "${monthFeedings.size}",
                            accentColor = CategoryFeedingColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "30d Diapers",
                            value = "${monthDiapers.size}",
                            accentColor = CategoryDiaperColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "30d Hygiene",
                            value = "${monthHygiene.size}",
                            accentColor = CategoryHygieneColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            label = "Current Weight",
                            value = latestGrowth?.weightKg?.let { "${it} kg" }
                                ?: uiState.activeBaby?.currentWeightKg?.let { "${it} kg" } ?: "--",
                            accentColor = SageGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Initial Weight",
                            value = earliestGrowth?.weightKg?.let { "${it} kg" }
                                ?: uiState.activeBaby?.birthWeightKg?.let { "${it} kg" } ?: "--",
                            accentColor = SoftPeachDark,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Current Height",
                            value = latestGrowth?.heightCm?.let { "${it} cm" }
                                ?: uiState.activeBaby?.currentHeightCm?.let { "${it} cm" } ?: "--",
                            accentColor = CategoryDiaperColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyDualBarChart(
    buckets: List<Triple<String, Int, Int>>,
    primaryColor: Color,
    secondaryColor: Color
) {
    val maxVal = (buckets.maxOfOrNull { maxOf(it.second, it.third) } ?: 1).coerceAtLeast(4)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val count = buckets.size.coerceAtLeast(1)
            val groupWidth = size.width / count
            val barWidth = (groupWidth * 0.28f).coerceAtLeast(6f)
            val chartHeight = size.height - 12f

            buckets.forEachIndexed { index, (_, feedCount, diaperCount) ->
                val centerX = index * groupWidth + groupWidth / 2f
                val feedH = (feedCount.toFloat() / maxVal.toFloat()) * chartHeight
                val diaperH = (diaperCount.toFloat() / maxVal.toFloat()) * chartHeight

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(centerX - barWidth - 3f, size.height - feedH.coerceAtLeast(6f)),
                    size = Size(barWidth, feedH.coerceAtLeast(6f)),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = secondaryColor,
                    topLeft = Offset(centerX + 3f, size.height - diaperH.coerceAtLeast(6f)),
                    size = Size(barWidth, diaperH.coerceAtLeast(6f)),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            buckets.forEach { (dayLabel, fCount, dCount) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(dayLabel, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$fCount/$dCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedActivityHistorySection(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    val now = uiState.currentTimeMillis
    val use24h = uiState.appSettings.use24HourFormat
    val datePattern = uiState.appSettings.dateFormatPattern

    val (rangeStart, rangeEnd) = remember(uiState.historyDateFilter, uiState.customStartDateMillis, uiState.customEndDateMillis, now) {
        when (uiState.historyDateFilter) {
            DateRangeFilter.TODAY -> DateTimeFormatters.startOfDayMillis(now) to DateTimeFormatters.endOfDayMillis(now)
            DateRangeFilter.YESTERDAY -> {
                val y = now - 86_400_000L
                DateTimeFormatters.startOfDayMillis(y) to DateTimeFormatters.endOfDayMillis(y)
            }
            DateRangeFilter.LAST_7_DAYS -> (now - 7 * 86_400_000L) to now
            DateRangeFilter.LAST_30_DAYS -> (now - 30 * 86_400_000L) to now
            DateRangeFilter.CUSTOM -> uiState.customStartDateMillis to uiState.customEndDateMillis
        }
    }

    val allUnifiedItems = remember(
        uiState.feedingRecords,
        uiState.diaperRecords,
        uiState.cleaningRecords,
        uiState.hygieneRecords,
        uiState.reminderCompletions,
        uiState.historyCategoryFilter,
        rangeStart,
        rangeEnd
    ) {
        val list = mutableListOf<UnifiedActivityItem>()
        if (uiState.historyCategoryFilter in setOf(UnifiedActivityCategory.ALL, UnifiedActivityCategory.FEEDING)) {
            uiState.feedingRecords.filter { it.startTimeMillis in rangeStart..rangeEnd }.forEach { r ->
                list += UnifiedActivityItem(
                    uniqueKey = "feed_${r.id}",
                    entityId = r.id,
                    category = UnifiedActivityCategory.FEEDING,
                    title = r.feedingType,
                    subtitle = "Feeding Session",
                    details = "${r.durationMinutes} min" + (r.milkAmountMl?.let { " • $it ml" } ?: "") +
                            if (r.notes.isNotBlank()) " • ${r.notes}" else "",
                    statusLabel = "COMPLETED",
                    timestampMillis = r.startTimeMillis
                )
            }
        }
        if (uiState.historyCategoryFilter in setOf(UnifiedActivityCategory.ALL, UnifiedActivityCategory.DIAPER)) {
            uiState.diaperRecords.filter { it.timestampMillis in rangeStart..rangeEnd }.forEach { r ->
                list += UnifiedActivityItem(
                    uniqueKey = "diaper_${r.id}",
                    entityId = r.id,
                    category = UnifiedActivityCategory.DIAPER,
                    title = "Diaper Change (${r.diaperType})",
                    subtitle = "Diaper Tracker",
                    details = r.notes.ifBlank { "Recorded ${r.diaperType} diaper" },
                    statusLabel = "COMPLETED",
                    timestampMillis = r.timestampMillis
                )
            }
        }
        if (uiState.historyCategoryFilter in setOf(UnifiedActivityCategory.ALL, UnifiedActivityCategory.CLEANING)) {
            uiState.cleaningRecords.filter { it.completedAtMillis in rangeStart..rangeEnd }.forEach { r ->
                list += UnifiedActivityItem(
                    uniqueKey = "clean_${r.id}",
                    entityId = r.id,
                    category = UnifiedActivityCategory.CLEANING,
                    title = r.activityName,
                    subtitle = "Equipment Cleaning & Sterilization",
                    details = "Repeat interval: ${r.repeatIntervalHours}h" +
                            if (r.notes.isNotBlank()) " • ${r.notes}" else "",
                    statusLabel = "CLEANED",
                    timestampMillis = r.completedAtMillis
                )
            }
        }
        if (uiState.historyCategoryFilter in setOf(UnifiedActivityCategory.ALL, UnifiedActivityCategory.HYGIENE)) {
            uiState.hygieneRecords.filter { it.timestampMillis in rangeStart..rangeEnd }.forEach { r ->
                list += UnifiedActivityItem(
                    uniqueKey = "hyg_${r.id}",
                    entityId = r.id,
                    category = UnifiedActivityCategory.HYGIENE,
                    title = r.activityName,
                    subtitle = "Baby Personal Hygiene",
                    details = r.notes.ifBlank { "Routine status: ${r.status}" },
                    statusLabel = r.status,
                    timestampMillis = r.timestampMillis
                )
            }
        }
        if (uiState.historyCategoryFilter in setOf(UnifiedActivityCategory.ALL, UnifiedActivityCategory.REMINDERS)) {
            uiState.reminderCompletions.filter { it.actionTimestampMillis in rangeStart..rangeEnd }.forEach { r ->
                list += UnifiedActivityItem(
                    uniqueKey = "rem_${r.id}",
                    entityId = r.id,
                    category = UnifiedActivityCategory.REMINDERS,
                    title = r.reminderTitle,
                    subtitle = "Reminder (${r.category})",
                    details = "Action taken: ${r.actionTaken}",
                    statusLabel = r.actionTaken,
                    timestampMillis = r.actionTimestampMillis
                )
            }
        }
        list.sortedByDescending { it.timestampMillis }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category Filter Chips
        item {
            Text("Filter by Category", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UnifiedActivityCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = uiState.historyCategoryFilter == cat,
                        onClick = { viewModel.setHistoryFilters(category = cat) },
                        label = { Text(cat.name) }
                    )
                }
            }
        }

        // Date Range Filter Chips
        item {
            Text("Filter by Date Range", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    DateRangeFilter.TODAY to "Today",
                    DateRangeFilter.YESTERDAY to "Yesterday",
                    DateRangeFilter.LAST_7_DAYS to "Last 7 Days",
                    DateRangeFilter.LAST_30_DAYS to "Last 30 Days"
                ).forEach { (dFilter, label) ->
                    FilterChip(
                        selected = uiState.historyDateFilter == dFilter,
                        onClick = { viewModel.setHistoryFilters(dateFilter = dFilter) },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            Text(
                text = "Matching Records (${allUnifiedItems.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (allUnifiedItems.isEmpty()) {
            item {
                CareEmptyStateCard(
                    icon = Icons.Outlined.EventNote,
                    title = "No activities found for selected filter",
                    message = "Try switching the date range to 'Last 30 Days' or 'All' categories."
                )
            }
        } else {
            items(allUnifiedItems, key = { it.uniqueKey }) { item ->
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
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${item.subtitle} • ${DateTimeFormatters.formatDateTime(item.timestampMillis, datePattern, use24h)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = SageGreenDark
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SageGreenContainer
                        ) {
                            Text(
                                text = item.statusLabel,
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
