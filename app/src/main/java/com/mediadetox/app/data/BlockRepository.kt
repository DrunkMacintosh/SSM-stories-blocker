package com.mediadetox.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class BlockRepository(private val db: AppDatabase) {

    private val dao = db.blockEventDao()

    // --- Logging helpers ---

    suspend fun logBlocked(packageName: String) = withContext(Dispatchers.IO) {
        dao.insert(BlockEvent(timestamp = System.currentTimeMillis(), eventType = "BLOCKED", appPackage = packageName))
    }

    suspend fun logCaved(packageName: String) = withContext(Dispatchers.IO) {
        dao.insert(BlockEvent(timestamp = System.currentTimeMillis(), eventType = "CAVED", appPackage = packageName))
    }

    suspend fun logDmBypass(packageName: String) = withContext(Dispatchers.IO) {
        dao.insert(BlockEvent(timestamp = System.currentTimeMillis(), eventType = "DM_BYPASS", appPackage = packageName))
    }

    // --- Queries ---

    suspend fun getEventsToday(): List<BlockEvent> = withContext(Dispatchers.IO) {
        dao.getEventsToday(startOfToday())
    }

    suspend fun getEventsThisWeek(): List<BlockEvent> = withContext(Dispatchers.IO) {
        dao.getEventsThisWeek(startOfWeek())
    }

    suspend fun countBlockedToday(): Int = withContext(Dispatchers.IO) {
        dao.countBlockedToday(startOfToday())
    }

    suspend fun countCavedToday(): Int = withContext(Dispatchers.IO) {
        dao.countCavedToday(startOfToday())
    }

    /** Returns how many consecutive days (including today) had zero CAVED events. */
    suspend fun getCurrentStreak(): Int = withContext(Dispatchers.IO) {
        var streak = 0
        val dayMs = 24 * 60 * 60 * 1000L
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (i in 0..29) {
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + dayMs
            if (dao.countCavedBetween(dayStart, dayEnd) == 0) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        streak
    }

    // --- Time helpers ---

    private fun startOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfWeek(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
