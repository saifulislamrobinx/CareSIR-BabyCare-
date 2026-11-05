package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.*
import com.example.util.DateTimeFormatters

private data class OnboardingPageData(
    val stepNumber: Int,
    val badge: String,
    val title: String,
    val subtitle: String,
    val highlights: List<String>,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingAndProfileSetupScreen(
    onCompleteSetup: (
        fullName: String,
        dobMillis: Long,
        gender: String,
        photoUri: String?,
        birthWeightKg: Float?,
        birthHeightCm: Float?,
        bloodGroup: String,
        birthNotes: String
    ) -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPageData(
                stepNumber = 1,
                badge = "WELCOME TO CARESIR",
                title = "Welcome to CareSIR",
                subtitle = "Your little one's daily care companion — Every Little Moment, Cared For.",
                highlights = listOf(
                    "Thoughtfully crafted for newborn & infant parents",
                    "100% private, offline-first local storage",
                    "Warm, calm interface designed for day & late-night care"
                ),
                icon = Icons.Filled.Favorite,
                accentColor = SageGreen
            ),
            OnboardingPageData(
                stepNumber = 2,
                badge = "COMPLETE DAILY TRACKING",
                title = "Track feeding, diaper changes, hygiene, and baby activities",
                subtitle = "Log breastfeeding sessions with a live timer, bottle volume, diaper checks, equipment sterilization, and newborn hygiene in seconds.",
                highlights = listOf(
                    "One-tap live Feeding Timer with Left/Right breast switcher",
                    "Wet, Dirty, Both & Dry diaper tracking with instant history",
                    "Bottle & breast pump cleaning and sterilization tracker"
                ),
                icon = Icons.Filled.ChildCare,
                accentColor = CategoryDiaperColor
            ),
            OnboardingPageData(
                stepNumber = 3,
                badge = "SMART REMINDER ENGINE",
                title = "Never miss important baby-care reminders",
                subtitle = "Customizable feeding intervals, recurring hygiene schedules, and unlimited personal reminders that adapt to your routine.",
                highlights = listOf(
                    "Automatic next-feeding calculation after every session",
                    "Done, Snooze 15m, and Skip reminder actions",
                    "Custom family & care routines with quiet hours support"
                ),
                icon = Icons.Filled.NotificationsActive,
                accentColor = CategoryReminderColor
            ),
            OnboardingPageData(
                stepNumber = 4,
                badge = "PERSONALIZED CARE",
                title = "Create your baby's profile and personalize your experience",
                subtitle = "Automatically calculate your baby's exact age in days, weeks, or months, and monitor weight and height growth trends over time.",
                highlights = listOf(
                    "Automatic age calculation from Date of Birth",
                    "Visual weight & height growth charts and reports",
                    "Optional fields can be skipped and completed anytime"
                ),
                icon = Icons.Filled.AutoGraph,
                accentColor = CategoryHygieneColor
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) } // 0..3 = onboarding, 4 = Baby Profile Setup

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentStep,
            label = "onboarding_transition"
        ) { step ->
            if (step < pages.size) {
                val page = pages[step]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top bar with Skip button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(SageGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChildCare,
                                    contentDescription = "CareSIR Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "CareSIR",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        TextButton(
                            onClick = { currentStep = pages.size },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("onboarding_skip_button")
                        ) {
                            Text("Skip to Profile Setup")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hero visual card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(185.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_nursery_banner),
                                    contentDescription = page.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(page.accentColor.copy(alpha = 0.22f))
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 26.dp)
                                        .size(64.dp),
                                    shape = CircleShape,
                                    color = page.accentColor,
                                    shadowElevation = 6.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = page.icon,
                                            contentDescription = page.title,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(36.dp))

                            Column(
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = page.accentColor.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = page.badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = page.accentColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }

                                Text(
                                    text = page.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = page.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    page.highlights.forEach { bullet ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = page.accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = bullet,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step indicators + Next button
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pages.indices.forEach { idx ->
                                val active = idx == step
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(if (active) 28.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) page.accentColor
                                            else MaterialTheme.colorScheme.outline
                                        )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (step > 0) {
                                OutlinedButton(
                                    onClick = { currentStep = step - 1 },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Back")
                                }
                            }
                            Button(
                                onClick = { currentStep = step + 1 },
                                modifier = Modifier
                                    .weight(2f)
                                    .defaultMinSize(minHeight = 52.dp)
                                    .testTag("onboarding_next_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                            ) {
                                Text(
                                    text = if (step == pages.lastIndex) "Set Up Baby Profile" else "Continue",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                BabyProfileSetupForm(
                    onBackToOnboarding = { currentStep = pages.lastIndex },
                    onCompleteSetup = onCompleteSetup
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BabyProfileSetupForm(
    onBackToOnboarding: () -> Unit,
    onCompleteSetup: (
        fullName: String,
        dobMillis: Long,
        gender: String,
        photoUri: String?,
        birthWeightKg: Float?,
        birthHeightCm: Float?,
        bloodGroup: String,
        birthNotes: String
    ) -> Unit
) {
    val context = LocalContext.current
    var babyName by remember { mutableStateOf("") }
    var dobMillis by remember { mutableLongStateOf(System.currentTimeMillis() - 14 * 86_400_000L) }
    var gender by remember { mutableStateOf("Girl") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var birthWeightText by remember { mutableStateOf("3.2") }
    var birthHeightText by remember { mutableStateOf("50.0") }
    var bloodGroup by remember { mutableStateOf("") }
    var birthNotes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            photoUri = uri.toString()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dobMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            dobMillis = it.coerceAtMost(System.currentTimeMillis())
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackToOnboarding) {
                Text("Back")
            }
            Text(
                text = "Baby Profile Setup",
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(
                onClick = {
                    val fallbackName = babyName.trim().ifBlank { "Little Star" }
                    onCompleteSetup(
                        fallbackName,
                        dobMillis,
                        gender,
                        photoUri,
                        birthWeightText.toFloatOrNull(),
                        birthHeightText.toFloatOrNull(),
                        bloodGroup,
                        birthNotes
                    )
                },
                modifier = Modifier.testTag("skip_optional_setup_button")
            ) {
                Text("Quick Start")
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar / Photo Picker
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(SageGreenContainer)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("profile_photo_picker_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Baby Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.AddAPhoto,
                                contentDescription = "Choose Baby Photo",
                                tint = SageGreenDark,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Add Photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = SageGreenDark
                            )
                        }
                    }
                }

                Text(
                    text = "Personalize CareSIR for your baby",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = babyName,
                    onValueChange = {
                        babyName = it
                        nameError = false
                    },
                    label = { Text("Baby's Full Name *") },
                    placeholder = { Text("e.g., Baby Ayla / Ayaan") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Please enter your baby's name") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_baby_name_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                // Date of Birth selector + quick presets
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Date of Birth • ${DateTimeFormatters.formatAgeFromDob(dobMillis)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("setup_dob_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateTimeFormatters.formatDate(dobMillis),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = "Pick Date of Birth",
                                tint = SageGreen
                            )
                        }
                    }

                    // Quick age presets for fast setup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Newborn" to 3L,
                            "2 Weeks" to 14L,
                            "1 Month" to 32L,
                            "3 Months" to 95L
                        ).forEach { (label, daysAgo) ->
                            AssistChip(
                                onClick = {
                                    dobMillis = System.currentTimeMillis() - daysAgo * 86_400_000L
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Gender Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Girl", "Boy", "Other").forEach { option ->
                            val selected = gender == option
                            FilterChip(
                                selected = selected,
                                onClick = { gender = option },
                                label = {
                                    Text(
                                        text = option,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("gender_chip_${option.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SageGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = SageGreenDark
                                )
                            )
                        }
                    }
                }

                // Optional Birth Weight & Height
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = birthWeightText,
                        onValueChange = { birthWeightText = it },
                        label = { Text("Birth Weight (kg)") },
                        placeholder = { Text("Optional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_birth_weight_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = birthHeightText,
                        onValueChange = { birthHeightText = it },
                        label = { Text("Birth Height (cm)") },
                        placeholder = { Text("Optional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_birth_height_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        label = { Text("Blood Group (Optional)") },
                        placeholder = { Text("e.g., O+") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = birthNotes,
                        onValueChange = { birthNotes = it },
                        label = { Text("Pediatric Notes (Optional)") },
                        placeholder = { Text("Any special notes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftPeachLight,
                    border = BorderStroke(1.dp, SoftPeach)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Private & Offline",
                            tint = SoftPeachDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "All baby records stay strictly private on your device. No account registration or internet required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkCharcoal
                        )
                    }
                }

                Button(
                    onClick = {
                        if (babyName.isBlank()) {
                            nameError = true
                        } else {
                            onCompleteSetup(
                                babyName.trim(),
                                dobMillis,
                                gender,
                                photoUri,
                                birthWeightText.toFloatOrNull(),
                                birthHeightText.toFloatOrNull(),
                                bloodGroup,
                                birthNotes
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 54.dp)
                        .testTag("complete_profile_setup_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Complete Setup & Open Dashboard",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
