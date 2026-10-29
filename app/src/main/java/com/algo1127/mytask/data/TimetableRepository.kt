package com.algo1127.mytask.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek

class TimetableRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = MyTaskDatabase.getDatabase(appContext)
    private val dao = database.timetableDao()

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("mytask_prefs", Context.MODE_PRIVATE)

    private val _showInEventsTab = MutableStateFlow(
        prefs.getBoolean(PREF_SHOW_IN_EVENTS_TAB, false)
    )
    val showInEventsTabFlow: StateFlow<Boolean> = _showInEventsTab

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PREF_SHOW_IN_EVENTS_TAB) {
            _showInEventsTab.value = prefs.getBoolean(PREF_SHOW_IN_EVENTS_TAB, false)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    fun getShowInEventsTab(): Boolean = _showInEventsTab.value

    fun setShowInEventsTab(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SHOW_IN_EVENTS_TAB, enabled).apply()
        _showInEventsTab.value = enabled
    }

    val allSlotsFlow: Flow<List<TimetableSlot>> = dao.getAllSlotsFlow()

    fun getSlotsForDayFlow(dayOfWeek: DayOfWeek): Flow<List<TimetableSlot>> =
        dao.getSlotsForDayFlow(dayOfWeek)

    suspend fun getSlotsForDay(dayOfWeek: DayOfWeek): List<TimetableSlot> =
        dao.getSlotsForDay(dayOfWeek)

    suspend fun insertSlot(slot: TimetableSlot) {
        dao.insertSlot(slot)
    }

    suspend fun insertSlots(slots: List<TimetableSlot>) {
        dao.insertSlots(slots)
    }

    suspend fun deleteSlot(slot: TimetableSlot) {
        dao.deleteSlot(slot)
    }

    suspend fun deleteSlots(slotIds: List<Long>) {
        dao.deleteSlotsByIds(slotIds)
    }

    suspend fun clearDay(dayOfWeek: DayOfWeek) {
        dao.deleteSlotsForDay(dayOfWeek)
    }

    suspend fun cloneDaySchedule(sourceDay: DayOfWeek, targetDays: List<DayOfWeek>) {
        val sourceSlots = dao.getSlotsForDay(sourceDay)
        if (sourceSlots.isEmpty()) return

        for (targetDay in targetDays) {
            dao.deleteSlotsForDay(targetDay)
            val newSlots = sourceSlots.map { slot ->
                slot.copy(
                    id = System.nanoTime() + (Math.random() * 1000).toLong(),
                    dayOfWeek = targetDay
                )
            }
            dao.insertSlots(newSlots)
        }
    }

    companion object {
        private const val PREF_SHOW_IN_EVENTS_TAB = "timetable_show_in_events_tab"

        @Volatile
        private var INSTANCE: TimetableRepository? = null

        fun getInstance(context: Context): TimetableRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: TimetableRepository(context.applicationContext).also { INSTANCE = it }
                instance
            }
        }
    }
}
