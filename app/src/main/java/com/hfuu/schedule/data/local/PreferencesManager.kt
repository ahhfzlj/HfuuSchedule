package com.hfuu.schedule.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hfuu.schedule.data.model.AccountConnectionState
import com.hfuu.schedule.data.model.HfuuDefaults
import com.hfuu.schedule.data.model.SemesterConfig
import com.hfuu.schedule.data.model.TimeSlot
import com.hfuu.schedule.data.model.UserProfile
import com.hfuu.schedule.ui.theme.AppUiStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.appDataStore by preferencesDataStore(name = "hfuu_schedule_prefs")

class PreferencesManager(private val context: Context) {

    private val quickPrefs = context.getSharedPreferences("hfuu_quick_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val timeSlotsFlow: Flow<List<TimeSlot>> = context.appDataStore.data.map { prefs ->
        val raw = prefs[KEY_TIME_SLOTS]
        if (raw.isNullOrBlank()) {
            HfuuDefaults.defaultTimeSlots
        } else {
            runCatching { json.decodeFromString<List<TimeSlot>>(raw) }
                .getOrDefault(HfuuDefaults.defaultTimeSlots)
        }
    }

    val userProfileFlow: Flow<UserProfile> = context.appDataStore.data.map { prefs ->
        val raw = prefs[KEY_USER_PROFILE]
        if (raw.isNullOrBlank()) {
            UserProfile()
        } else {
            runCatching { json.decodeFromString<UserProfile>(raw) }
                .getOrDefault(UserProfile())
        }
    }

    val semesterConfigFlow: Flow<SemesterConfig> = context.appDataStore.data.map { prefs ->
        val raw = prefs[KEY_SEMESTER_CONFIG]
        if (raw.isNullOrBlank()) {
            SemesterConfig()
        } else {
            runCatching { json.decodeFromString<SemesterConfig>(raw) }
                .getOrDefault(SemesterConfig())
        }
    }

    val accountStateFlow: Flow<AccountConnectionState> = context.appDataStore.data.map { prefs ->
        val raw = prefs[KEY_ACCOUNT_STATE]
        if (raw.isNullOrBlank()) {
            AccountConnectionState()
        } else {
            runCatching { json.decodeFromString<AccountConnectionState>(raw) }
                .getOrDefault(AccountConnectionState())
        }
    }

    val hideWeekendsFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[KEY_HIDE_WEEKENDS] ?: false
    }

    val memoEnabledFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[KEY_MEMO_ENABLED] ?: false
    }

    val uiStyleFlow: Flow<AppUiStyle> = context.appDataStore.data.map { prefs ->
        AppUiStyle.fromId(prefs[KEY_UI_STYLE])
    }

    val startInAgendaFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[KEY_START_IN_AGENDA] ?: false
    }

    fun getStartInAgendaSync(): Boolean = quickPrefs.getBoolean("start_in_agenda", false)

    suspend fun setHideWeekends(hide: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_HIDE_WEEKENDS] = hide
        }
    }

    suspend fun setMemoEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_MEMO_ENABLED] = enabled
        }
    }

    suspend fun setUiStyle(style: AppUiStyle) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_UI_STYLE] = style.id
        }
    }

    suspend fun setStartInAgenda(enabled: Boolean) {
        quickPrefs.edit().putBoolean("start_in_agenda", enabled).apply()
        context.appDataStore.edit { prefs ->
            prefs[KEY_START_IN_AGENDA] = enabled
        }
    }

    suspend fun saveTimeSlots(slots: List<TimeSlot>) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_TIME_SLOTS] = json.encodeToString(slots)
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_USER_PROFILE] = json.encodeToString(profile)
        }
    }

    suspend fun saveSemesterConfig(config: SemesterConfig) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_SEMESTER_CONFIG] = json.encodeToString(config)
        }
    }

    suspend fun updateAccountState(transform: (AccountConnectionState) -> AccountConnectionState) {
        context.appDataStore.edit { prefs ->
            val current = prefs[KEY_ACCOUNT_STATE]?.let {
                runCatching { json.decodeFromString<AccountConnectionState>(it) }.getOrNull()
            } ?: AccountConnectionState()
            prefs[KEY_ACCOUNT_STATE] = json.encodeToString(transform(current))
        }
    }

    suspend fun clearAll() {
        quickPrefs.edit().clear().apply()
        context.appDataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_TIME_SLOTS = stringPreferencesKey("time_slots_json")
        private val KEY_USER_PROFILE = stringPreferencesKey("user_profile_json")
        private val KEY_SEMESTER_CONFIG = stringPreferencesKey("semester_config_json")
        private val KEY_ACCOUNT_STATE = stringPreferencesKey("account_state_json")
        private val KEY_HIDE_WEEKENDS = booleanPreferencesKey("hide_weekends")
        private val KEY_MEMO_ENABLED = booleanPreferencesKey("memo_enabled")
        private val KEY_UI_STYLE = stringPreferencesKey("ui_style_id")
        private val KEY_START_IN_AGENDA = booleanPreferencesKey("start_in_agenda")
    }
}
