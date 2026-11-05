package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CareSirDatabase
import com.example.data.repository.CareSirRepository
import com.example.notifications.CareNotificationManager
import com.example.ui.screens.*
import com.example.ui.theme.CareSIRTheme
import com.example.ui.theme.SageGreen
import com.example.ui.viewmodel.CareSirViewModel
import com.example.util.BanglaStrings
import com.example.util.EnglishStrings
import com.example.util.LocalAppStrings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current.applicationContext
            val repository = remember {
                CareSirRepository(CareSirDatabase.getInstance(context).careSirDao())
            }
            val careViewModel: CareSirViewModel = viewModel(
                factory = CareSirViewModel.Factory(repository, context)
            )
            CareSirAppRoot(viewModel = careViewModel)
        }
    }
}

@Composable
fun CareSirAppRoot(viewModel: CareSirViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activeStrings = if (uiState.appSettings.languageCode == "bn") BanglaStrings else EnglishStrings

    // Request Android 13+ POST_NOTIFICATIONS permission gracefully after onboarding
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(uiState.appSettings.onboardingCompleted) {
        if (uiState.appSettings.onboardingCompleted &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !CareNotificationManager.hasNotificationPermission(context)
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var currentNavIndex by remember { mutableIntStateOf(0) } // 0=Home, 1=Activities, 2=Reminders, 3=Reports, 4=Profile
    var showQuickActionSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearBanner()
        }
    }

    if (currentNavIndex != 0) {
        BackHandler {
            currentNavIndex = 0
        }
    }

    CompositionLocalProvider(LocalAppStrings provides activeStrings) {
        CareSIRTheme(themeMode = uiState.appSettings.themeMode) {
            if (!uiState.appSettings.onboardingCompleted && uiState.activeBaby == null) {
                OnboardingAndProfileSetupScreen(
                    onCompleteSetup = { fullName, dobMillis, gender, photoUri, bWeight, bHeight, blood, notes ->
                        viewModel.saveBabyProfile(
                            id = 0L,
                            fullName = fullName,
                            dateOfBirthMillis = dobMillis,
                            gender = gender,
                            photoUri = photoUri,
                            birthWeightKg = bWeight,
                            birthHeightCm = bHeight,
                            currentWeightKg = bWeight,
                            currentHeightCm = bHeight,
                            bloodGroup = blood,
                            birthNotes = notes
                        )
                    }
                )
            } else {
                if (showQuickActionSheet) {
                    AlertDialog(
                        onDismissRequest = { showQuickActionSheet = false },
                        title = { Text(activeStrings.quickActionsTitle) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        showQuickActionSheet = false
                                        viewModel.selectActivityTab(0)
                                        currentNavIndex = 1
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(activeStrings.logFeeding)
                                }
                                Button(
                                    onClick = {
                                        showQuickActionSheet = false
                                        viewModel.saveDiaperRecord(diaperType = "Wet")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${activeStrings.logDiaper} (Wet)")
                                }
                                Button(
                                    onClick = {
                                        showQuickActionSheet = false
                                        viewModel.saveHygieneRecord(activityName = "Bath Time", status = "COMPLETED")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${activeStrings.completeHygiene} (Bath Time)")
                                }
                                Button(
                                    onClick = {
                                        showQuickActionSheet = false
                                        viewModel.saveEquipmentCleaningRecord(activityName = "Feeding Bottle Sterilization")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${activeStrings.cleanEquipment} (Bottle Sterilization)")
                                }
                                OutlinedButton(
                                    onClick = {
                                        showQuickActionSheet = false
                                        currentNavIndex = 2
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(activeStrings.addReminder)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showQuickActionSheet = false }) {
                                Text(activeStrings.cancel)
                            }
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showQuickActionSheet = true },
                            containerColor = SageGreen,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("main_quick_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = activeStrings.quickActionsTitle
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            val navItems = listOf(
                                Triple(activeStrings.navHome, Icons.Filled.Home, Icons.Outlined.Home),
                                Triple(activeStrings.navActivities, Icons.Filled.ChildCare, Icons.Outlined.ChildCare),
                                Triple(activeStrings.navReminders, Icons.Filled.Notifications, Icons.Outlined.Notifications),
                                Triple(activeStrings.navReports, Icons.Filled.InsertChart, Icons.Outlined.InsertChartOutlined),
                                Triple(activeStrings.navProfile, Icons.Filled.Person, Icons.Outlined.Person)
                            )
                            navItems.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                                val selected = currentNavIndex == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentNavIndex = index },
                                    modifier = Modifier.testTag("bottom_nav_$index"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) filledIcon else outlinedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentNavIndex) {
                            0 -> DashboardScreen(
                                uiState = uiState,
                                onOpenProfile = { currentNavIndex = 4 },
                                onOpenNotifications = { currentNavIndex = 2 },
                                onOpenFeedingTab = {
                                    viewModel.selectActivityTab(0)
                                    currentNavIndex = 1
                                },
                                onOpenDiaperTab = {
                                    viewModel.selectActivityTab(1)
                                    currentNavIndex = 1
                                },
                                onOpenCleaningTab = {
                                    viewModel.selectActivityTab(2)
                                    currentNavIndex = 1
                                },
                                onOpenHygieneTab = {
                                    viewModel.selectActivityTab(3)
                                    currentNavIndex = 1
                                },
                                onOpenRemindersScreen = { currentNavIndex = 2 },
                                onStartFeedingTimer = { side -> viewModel.startFeedingTimer(side) },
                                onPauseResumeTimer = { viewModel.pauseOrResumeFeedingTimer() },
                                onStopAndSaveTimer = { viewModel.stopAndSaveFeedingTimer() },
                                onQuickLogFeeding = {
                                    val now = System.currentTimeMillis()
                                    viewModel.saveFeedingRecord(
                                        id = 0L,
                                        feedingType = "Breastfeeding – Left",
                                        startTimeMillis = now - 15 * 60_000L,
                                        endTimeMillis = now,
                                        durationSeconds = 900L,
                                        milkAmountMl = null,
                                        breastSide = "Left",
                                        notes = "Quick logged from Dashboard"
                                    )
                                },
                                onQuickLogDiaper = { dType ->
                                    viewModel.saveDiaperRecord(diaperType = dType)
                                },
                                onQuickCompleteHygiene = { actName ->
                                    viewModel.saveHygieneRecord(activityName = actName, status = "COMPLETED")
                                },
                                onQuickCleanEquipment = { eqName ->
                                    viewModel.saveEquipmentCleaningRecord(activityName = eqName)
                                },
                                onReminderAction = { rem, act ->
                                    viewModel.performReminderAction(rem, act)
                                },
                                onCategoryAlarmAction = { cat, act ->
                                    viewModel.snoozeOrSkipCategoryAlarm(cat, act)
                                }
                            )
                            1 -> ActivitiesScreen(uiState = uiState, viewModel = viewModel)
                            2 -> RemindersScreen(uiState = uiState, viewModel = viewModel)
                            3 -> ReportsAndHistoryScreen(uiState = uiState, viewModel = viewModel)
                            else -> ProfileAndSettingsScreen(uiState = uiState, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
