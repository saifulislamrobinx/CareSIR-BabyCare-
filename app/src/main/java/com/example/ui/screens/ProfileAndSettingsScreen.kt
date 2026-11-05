package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CareSirUiState
import com.example.ui.viewmodel.CareSirViewModel
import com.example.util.DateTimeFormatters
import com.example.util.LocalAppStrings
import kotlinx.coroutines.launch

@Composable
fun ProfileAndSettingsScreen(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Baby Profile & Growth, 1 = Settings & Backup

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                modifier = Modifier
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag("profile_tab_baby"),
                text = { Text("Baby Profile & Growth") },
                icon = { Icon(Icons.Outlined.ChildCare, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                modifier = Modifier
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag("profile_tab_settings"),
                text = { Text("Settings & Backup") },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (subTab == 0) {
            BabyProfileAndGrowthSection(uiState = uiState, viewModel = viewModel)
        } else {
            SettingsAndBackupSection(uiState = uiState, viewModel = viewModel)
        }
    }
}

@Composable
private fun BabyProfileAndGrowthSection(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val baby = uiState.activeBaby
    val now = uiState.currentTimeMillis
    val datePattern = uiState.appSettings.dateFormatPattern

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddBabyDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf(baby?.currentWeightKg?.toString() ?: "4.5") }
    var heightInput by remember { mutableStateOf(baby?.currentHeightCm?.toString() ?: "55.0") }
    var growthNotes by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && baby != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.saveBabyProfile(
                id = baby.id,
                fullName = baby.fullName,
                dateOfBirthMillis = baby.dateOfBirthMillis,
                gender = baby.gender,
                photoUri = uri.toString(),
                birthWeightKg = baby.birthWeightKg,
                birthHeightCm = baby.birthHeightCm,
                currentWeightKg = baby.currentWeightKg,
                currentHeightCm = baby.currentHeightCm,
                bloodGroup = baby.bloodGroup,
                birthNotes = baby.birthNotes,
                additionalNotes = baby.additionalNotes
            )
        }
    }

    if (showEditProfileDialog || showAddBabyDialog) {
        val editingTarget = if (showAddBabyDialog) null else baby
        var name by remember { mutableStateOf(editingTarget?.fullName ?: "") }
        var gender by remember { mutableStateOf(editingTarget?.gender ?: "Girl") }
        var bWeight by remember { mutableStateOf(editingTarget?.birthWeightKg?.toString() ?: "3.2") }
        var bHeight by remember { mutableStateOf(editingTarget?.birthHeightCm?.toString() ?: "50.0") }
        var cWeight by remember { mutableStateOf(editingTarget?.currentWeightKg?.toString() ?: "4.2") }
        var cHeight by remember { mutableStateOf(editingTarget?.currentHeightCm?.toString() ?: "54.0") }
        var blood by remember { mutableStateOf(editingTarget?.bloodGroup ?: "") }
        var bNotes by remember { mutableStateOf(editingTarget?.birthNotes ?: "") }
        var extraNotes by remember { mutableStateOf(editingTarget?.additionalNotes ?: "") }

        AlertDialog(
            onDismissRequest = {
                showEditProfileDialog = false
                showAddBabyDialog = false
            },
            title = { Text(if (editingTarget == null) "Add Another Baby Profile" else "Edit Baby Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Girl", "Boy", "Other").forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(g) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bWeight,
                            onValueChange = { bWeight = it },
                            label = { Text("Birth Wt (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bHeight,
                            onValueChange = { bHeight = it },
                            label = { Text("Birth Ht (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cWeight,
                            onValueChange = { cWeight = it },
                            label = { Text("Current Wt (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cHeight,
                            onValueChange = { cHeight = it },
                            label = { Text("Current Ht (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = blood,
                        onValueChange = { blood = it },
                        label = { Text("Blood Group (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bNotes,
                        onValueChange = { bNotes = it },
                        label = { Text("Birth / Pediatric Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = extraNotes,
                        onValueChange = { extraNotes = it },
                        label = { Text("Additional Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.saveBabyProfile(
                            id = editingTarget?.id ?: 0L,
                            fullName = name,
                            dateOfBirthMillis = editingTarget?.dateOfBirthMillis ?: (now - 14 * 86_400_000L),
                            gender = gender,
                            photoUri = editingTarget?.photoUri,
                            birthWeightKg = bWeight.toFloatOrNull(),
                            birthHeightCm = bHeight.toFloatOrNull(),
                            currentWeightKg = cWeight.toFloatOrNull(),
                            currentHeightCm = cHeight.toFloatOrNull(),
                            bloodGroup = blood,
                            birthNotes = bNotes,
                            additionalNotes = extraNotes
                        )
                        showEditProfileDialog = false
                        showAddBabyDialog = false
                    }
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditProfileDialog = false
                    showAddBabyDialog = false
                }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Multi-baby profile switcher row
        if (uiState.allBabies.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.allBabies.forEach { profile ->
                        FilterChip(
                            selected = profile.id == baby?.id,
                            onClick = { viewModel.switchBabyProfile(profile.id) },
                            label = { Text(profile.fullName) },
                            leadingIcon = {
                                Icon(Icons.Filled.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                    AssistChip(
                        onClick = { showAddBabyDialog = true },
                        label = { Text("+ Add Baby") }
                    )
                }
            }
        }

        // Detailed Baby Profile Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(SageGreenContainer)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
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
                                        imageVector = Icons.Outlined.AddAPhoto,
                                        contentDescription = "Change Photo",
                                        tint = SageGreenDark,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = baby?.fullName ?: "Little Star",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = DateTimeFormatters.formatAgeFromDob(
                                        dobMillis = baby?.dateOfBirthMillis ?: now,
                                        nowMillis = now,
                                        isBangla = strings.isBangla
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SageGreenDark
                                )
                                Text(
                                    text = "DOB: ${DateTimeFormatters.formatDate(baby?.dateOfBirthMillis, datePattern)} • Gender: ${baby?.gender ?: "--"}" +
                                            if (!baby?.bloodGroup.isNullOrBlank()) " • Blood: ${baby?.bloodGroup}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalIconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.testTag("edit_baby_profile_button")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit Baby Profile")
                        }
                    }

                    val latestGrowth = uiState.growthRecords.firstOrNull()
                    val previousGrowth = uiState.growthRecords.getOrNull(1)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            label = "Birth Weight",
                            value = baby?.birthWeightKg?.let { "${it} kg" } ?: "--",
                            accentColor = SoftPeachDark,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Previous Wt",
                            value = previousGrowth?.weightKg?.let { "${it} kg" } ?: "--",
                            accentColor = SageGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Current Wt",
                            value = (latestGrowth?.weightKg ?: baby?.currentWeightKg)?.let { "${it} kg" } ?: "--",
                            accentColor = SageGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            label = "Birth Height",
                            value = baby?.birthHeightCm?.let { "${it} cm" } ?: "--",
                            accentColor = CategoryDiaperColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Previous Ht",
                            value = previousGrowth?.heightCm?.let { "${it} cm" } ?: "--",
                            accentColor = CategoryDiaperColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = "Current Ht",
                            value = (latestGrowth?.heightCm ?: baby?.currentHeightCm)?.let { "${it} cm" } ?: "--",
                            accentColor = CategoryDiaperColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!baby?.birthNotes.isNullOrBlank() || !baby?.additionalNotes.isNullOrBlank()) {
                        Text(
                            text = "Notes: ${listOfNotNull(baby?.birthNotes, baby?.additionalNotes).filter { it.isNotBlank() }.joinToString(" • ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Growth Measurement Entry & Trend Charts
        item {
            CareSectionCard(
                title = "Growth Tracking & Trend Chart",
                subtitle = "Log periodic weight (kg) and height (cm) measurements",
                icon = Icons.Filled.TrendingUp,
                accentColor = SageGreen
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = growthNotes,
                        onValueChange = { growthNotes = it },
                        label = { Text("Measurement Notes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val w = weightInput.toFloatOrNull() ?: return@Button
                            val h = heightInput.toFloatOrNull() ?: return@Button
                            viewModel.addGrowthMeasurement(w, h, growthNotes)
                            growthNotes = ""
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("add_growth_record_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                    ) {
                        Text("Log Growth")
                    }
                }

                if (uiState.growthRecords.size >= 2) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Weight (Green) & Height (Blue) Progression",
                        style = MaterialTheme.typography.labelLarge
                    )
                    GrowthTrendChartCanvas(records = uiState.growthRecords.reversed())
                }
            }
        }

        item {
            Text(
                text = "Growth History (${uiState.growthRecords.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(uiState.growthRecords, key = { it.id }) { gRecord ->
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
                            text = "${gRecord.weightKg} kg • ${gRecord.heightCm} cm",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = DateTimeFormatters.formatDate(gRecord.recordedAt, datePattern) +
                                    if (gRecord.notes.isNotBlank()) " • ${gRecord.notes}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.deleteGrowthMeasurement(gRecord) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete Growth", tint = ErrorMutedRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthTrendChartCanvas(records: List<com.example.data.local.GrowthRecordEntity>) {
    val minW = (records.minOfOrNull { it.weightKg } ?: 2f) - 0.5f
    val maxW = (records.maxOfOrNull { it.weightKg } ?: 8f) + 0.5f
    val minH = (records.minOfOrNull { it.heightCm } ?: 45f) - 2f
    val maxH = (records.maxOfOrNull { it.heightCm } ?: 75f) + 2f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 8.dp)
    ) {
        if (records.size < 2) return@Canvas
        val stepX = size.width / (records.size - 1).coerceAtLeast(1)
        val weightPath = Path()
        val heightPath = Path()

        records.forEachIndexed { idx, rec ->
            val x = idx * stepX
            val wy = size.height - ((rec.weightKg - minW) / (maxW - minW).coerceAtLeast(0.1f)) * (size.height - 20f) - 10f
            val hy = size.height - ((rec.heightCm - minH) / (maxH - minH).coerceAtLeast(0.1f)) * (size.height - 20f) - 10f

            if (idx == 0) {
                weightPath.moveTo(x, wy)
                heightPath.moveTo(x, hy)
            } else {
                weightPath.lineTo(x, wy)
                heightPath.lineTo(x, hy)
            }
            drawCircle(color = SageGreen, radius = 6f, center = Offset(x, wy))
            drawCircle(color = CategoryDiaperColor, radius = 6f, center = Offset(x, hy))
        }

        drawPath(path = weightPath, color = SageGreen, style = Stroke(width = 4f))
        drawPath(path = heightPath, color = CategoryDiaperColor, style = Stroke(width = 4f))
    }
}

@Composable
private fun SettingsAndBackupSection(
    uiState: CareSirUiState,
    viewModel: CareSirViewModel
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val appSettings = uiState.appSettings
    val notifSettings = uiState.notificationSettings

    var backupJsonDialogContent by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputJson by remember { mutableStateOf("") }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    if (backupJsonDialogContent != null) {
        AlertDialog(
            onDismissRequest = { backupJsonDialogContent = null },
            title = { Text("Local Backup & Export JSON") },
            text = {
                OutlinedTextField(
                    value = backupJsonDialogContent!!,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    restoreInputJson = backupJsonDialogContent!!
                    backupJsonDialogContent = null
                }) {
                    Text("Copy to Restore Buffer & Close")
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore CareSIR Backup JSON") },
            text = {
                OutlinedTextField(
                    value = restoreInputJson,
                    onValueChange = { restoreInputJson = it },
                    label = { Text("Paste CareSIR Backup JSON") },
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importBackupJson(restoreInputJson) {
                        showRestoreDialog = false
                    }
                }) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("CareSIR Privacy Policy & Terms") },
            text = {
                Text(
                    "CareSIR – Smart Baby Care & Parenting Assistant is strictly offline-first and privacy-focused.\n\n" +
                            "1. All baby profiles, photos, feeding logs, diaper records, hygiene checklists, and custom reminders are stored locally on your device using Room SQLite Database.\n" +
                            "2. CareSIR never transmits sensitive family or baby information to external servers.\n" +
                            "3. Medical Safety: Feeding intervals, sterilization timers, and analytics are customizable parenting tools and do not replace professional medical advice from your pediatrician."
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) { Text("Close") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. General Settings (Language, Theme, Time & Date Format)
        item {
            CareSectionCard(
                title = strings.settingsTitle,
                subtitle = "Language (English / বাংলা), Theme, and Date/Time formats",
                icon = Icons.Outlined.Tune,
                accentColor = SageGreen
            ) {
                Text("App Language", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("en" to "English", "bn" to "বাংলা (Bangla)").forEach { (code, label) ->
                        FilterChip(
                            selected = appSettings.languageCode == code,
                            onClick = {
                                viewModel.updateAppSettings { it.copy(languageCode = code) }
                            },
                            label = { Text(label) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("lang_chip_$code")
                        )
                    }
                }

                Text("App Theme", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "System").forEach { (mode, label) ->
                        FilterChip(
                            selected = appSettings.themeMode == mode,
                            onClick = {
                                viewModel.updateAppSettings { it.copy(themeMode = mode) }
                            },
                            label = { Text(label) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("theme_chip_${mode.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("24-Hour Time Format", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = appSettings.use24HourFormat,
                        onCheckedChange = { use24 ->
                            viewModel.updateAppSettings { it.copy(use24HourFormat = use24) }
                        }
                    )
                }

                Text("Date Format", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("dd MMM yyyy", "MMM dd, yyyy", "yyyy-MM-dd").forEach { fmt ->
                        FilterChip(
                            selected = appSettings.dateFormatPattern == fmt,
                            onClick = {
                                viewModel.updateAppSettings { it.copy(dateFormatPattern = fmt) }
                            },
                            label = { Text(fmt, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Notification Channel Settings & Quiet Hours
        item {
            CareSectionCard(
                title = "Notification Channels & Quiet Hours",
                subtitle = "Control individual baby-care alert categories",
                icon = Icons.Outlined.NotificationsActive,
                accentColor = CategoryReminderColor
            ) {
                val toggles = listOf(
                    Triple("Feeding Notifications", notifSettings.feedingNotificationsEnabled) { v: Boolean ->
                        viewModel.updateNotificationSettings { it.copy(feedingNotificationsEnabled = v) }
                    },
                    Triple("Diaper Notifications", notifSettings.diaperNotificationsEnabled) { v: Boolean ->
                        viewModel.updateNotificationSettings { it.copy(diaperNotificationsEnabled = v) }
                    },
                    Triple("Hygiene Notifications", notifSettings.hygieneNotificationsEnabled) { v: Boolean ->
                        viewModel.updateNotificationSettings { it.copy(hygieneNotificationsEnabled = v) }
                    },
                    Triple("Equipment Cleaning Notifications", notifSettings.cleaningNotificationsEnabled) { v: Boolean ->
                        viewModel.updateNotificationSettings { it.copy(cleaningNotificationsEnabled = v) }
                    },
                    Triple("Custom Reminder Notifications", notifSettings.customNotificationsEnabled) { v: Boolean ->
                        viewModel.updateNotificationSettings { it.copy(customNotificationsEnabled = v) }
                    },
                    Triple("Notification Sound", notifSettings.soundEnabled) { v: Boolean ->
                        viewModel.updateNotificationSettings { it.copy(soundEnabled = v) }
                    },
                    Triple("Vibration Support", notifSettings.vibrationEnabled) { v: Boolean ->
                        viewModel.updateNotificationSettings { it.copy(vibrationEnabled = v) }
                    },
                    Triple("Quiet Hours (22:00 – 06:00)", notifSettings.quietHoursEnabled) { v: Boolean ->
                        viewModel.updateNotificationSettings { it.copy(quietHoursEnabled = v) }
                    }
                )

                toggles.forEach { (label, checked, onChange) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = checked, onCheckedChange = onChange)
                    }
                }
            }
        }

        // 3. Data Backup, Restore, Export & About CareSIR
        item {
            CareSectionCard(
                title = "Local Data Backup, Restore & Privacy",
                subtitle = appSettings.lastBackupTimestamp?.let {
                    "Last backup: ${DateTimeFormatters.formatDateTime(it, appSettings.dateFormatPattern, appSettings.use24HourFormat)}"
                } ?: "Offline-first encrypted-ready JSON snapshot",
                icon = Icons.Outlined.Backup,
                accentColor = CategoryCleaningColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                backupJsonDialogContent = viewModel.exportBackupJson()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("backup_data_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                    ) {
                        Text(strings.backupData)
                    }
                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("restore_data_button")
                    ) {
                        Text(strings.restoreData)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CareSIR – Smart Baby Care & Parenting Assistant",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Version 1.0.0 • Offline-First • Every Little Moment, Cared For.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showPrivacyPolicyDialog = true }) {
                        Text("Privacy & Terms")
                    }
                }
            }
        }
    }
}
