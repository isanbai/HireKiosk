package id.hirejob.kiosk.core

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("settings")

class Prefs(private val context: Context) {

    companion object {
        val VIDEO_URI = stringPreferencesKey("video_uri")
        val IMAGE_URI = stringPreferencesKey("image_uri")
        val LOOP_VIDEO = booleanPreferencesKey("loop_video")
        val TRIGGER_TYPE = stringPreferencesKey("trigger_type")
        val DEBOUNCE_MS = intPreferencesKey("debounce_ms")
        val MIN_ACTIVE_MS = intPreferencesKey("min_active_ms")
        val MIN_IDLE_MS = intPreferencesKey("min_idle_ms")
        val AUTOSTART = booleanPreferencesKey("autostart")
        val KIOSK_MODE = booleanPreferencesKey("kiosk_mode")
        val BLE_SERVICE_UUID = stringPreferencesKey("ble_service_uuid")
        val BLE_CHAR_UUID = stringPreferencesKey("ble_char_uuid")
        val DIAGNOSTIC = booleanPreferencesKey("diagnostic")
    }

    fun <T> get(key: Preferences.Key<T>, default: T): Flow<T> =
        context.dataStore.data.map { it[key] ?: default }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }
}
