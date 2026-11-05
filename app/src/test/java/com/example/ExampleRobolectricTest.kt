package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CareSirDatabase
import com.example.data.local.NotificationSettingsEntity
import com.example.data.repository.CareSirRepository
import com.example.notifications.CareNotificationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: CareSirDatabase
    private lateinit var repository: CareSirRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, CareSirDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CareSirRepository(database.careSirDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify app_name resource is CareSIR`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("CareSIR", appName)
    }

    @Test
    fun `full baby care CRUD workflow and backup restore persistence`() = runBlocking {
        repository.ensureDefaultsInitialized()

        // 1. Create Baby Profile
        val now = System.currentTimeMillis()
        val babyId = repository.createOrUpdateBabyProfile(
            fullName = "Baby Ayla",
            dateOfBirthMillis = now - 14 * 86_400_000L,
            gender = "Girl",
            photoUri = null,
            birthWeightKg = 3.2f,
            birthHeightCm = 50.0f,
            currentWeightKg = 3.6f,
            currentHeightCm = 52.0f,
            bloodGroup = "O+",
            birthNotes = "Healthy newborn"
        )
        assertTrue(babyId > 0L)
        val activeBaby = repository.activeBaby.first()
        assertNotNull(activeBaby)
        assertEquals("Baby Ayla", activeBaby?.fullName)

        // 2. Add Feeding Record & verify next expected feeding reminder calculation
        val (_, nextFeedingMillis) = repository.saveFeedingRecord(
            babyId = babyId,
            feedingType = "Breastfeeding – Left",
            startTimeMillis = now - 900_000L,
            endTimeMillis = now,
            durationSeconds = 900L,
            milkAmountMl = null,
            breastSide = "Left",
            notes = "Good latch"
        )
        assertNotNull(nextFeedingMillis)
        assertTrue(nextFeedingMillis!! > now)
        val feedings = repository.observeFeedingRecords(babyId).first()
        assertEquals(1, feedings.size)

        // 3. Add Diaper Record
        val (_, nextDiaperMillis) = repository.saveDiaperRecord(
            babyId = babyId,
            diaperType = "Wet",
            timestampMillis = now,
            notes = "Normal"
        )
        assertNotNull(nextDiaperMillis)
        val diapers = repository.observeDiaperRecords(babyId).first()
        assertEquals(1, diapers.size)

        // 4. Add Equipment Cleaning Record
        repository.saveEquipmentCleaningRecord(
            babyId = babyId,
            activityName = "Feeding Bottle Sterilization",
            completedAtMillis = now,
            repeatIntervalHours = 4,
            reminderEnabled = true,
            isCustomEquipment = false,
            notes = "Steam sterilized"
        )
        val cleanings = repository.observeEquipmentCleaningRecords(babyId).first()
        assertEquals(1, cleanings.size)

        // 5. Add Baby Hygiene Record
        repository.saveHygieneRecord(
            babyId = babyId,
            activityName = "Bath Time",
            status = "COMPLETED",
            timestampMillis = now,
            notes = "Warm sponge bath",
            reminderEnabled = true,
            repeatIntervalHours = 24,
            isCustomActivity = false
        )
        val hygienes = repository.observeHygieneRecords(babyId).first()
        assertEquals(1, hygienes.size)

        // 6. Add Custom Reminder and test Snooze & Completion actions
        val savedReminder = repository.saveCustomReminder(
            babyId = babyId,
            title = "Morning sunlight routine",
            description = "10 minutes gentle morning light",
            scheduledTimeMillis = now + 3_600_000L,
            repeatType = "DAILY",
            category = "CUSTOM",
            priority = "HIGH"
        )
        assertTrue(savedReminder.id > 0L)

        val snoozed = repository.handleReminderAction(
            reminderId = savedReminder.id,
            title = savedReminder.title,
            category = savedReminder.category,
            action = "SNOOZED"
        )
        assertNotNull(snoozed)
        assertFalse(snoozed!!.isCompleted)

        val completed = repository.handleReminderAction(
            reminderId = savedReminder.id,
            title = savedReminder.title,
            category = savedReminder.category,
            action = "COMPLETED"
        )
        assertNotNull(completed)
        // Because repeatType is DAILY, it reschedules to tomorrow and stays active
        assertFalse(completed!!.isCompleted)
        assertTrue(completed.scheduledTimeMillis > now)

        // 7. Test Backup & Restore JSON round-trip
        val backupJson = repository.exportBackupJson()
        assertTrue(backupJson.contains("Baby Ayla"))
        assertTrue(backupJson.contains("Morning sunlight routine"))

        database.careSirDao().clearAllBabyProfiles()
        assertNull(repository.activeBaby.first())

        val restoredOk = repository.importBackupJson(backupJson)
        assertTrue(restoredOk)
        val restoredBaby = repository.activeBaby.first()
        assertNotNull(restoredBaby)
        assertEquals("Baby Ayla", restoredBaby?.fullName)
    }

    @Test
    fun `notification channels creation and quiet hours evaluation`() {
        CareNotificationManager.createNotificationChannels(context)

        val settings = NotificationSettingsEntity(
            quietHoursEnabled = true,
            quietHoursStartHour = 22,
            quietHoursEndHour = 6
        )
        val lateNight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }.timeInMillis
        val midday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        assertTrue(CareNotificationManager.isWithinQuietHours(settings, lateNight))
        assertFalse(CareNotificationManager.isWithinQuietHours(settings, midday))
    }
}
