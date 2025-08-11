package id.hirejob.kiosk.core

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("kiosk_prefs")

enum class TriggerType { USB_HID, BT_HID, HEADSET, HTTP, BLE_GATT, VOLUME }

object Prefs {
    val VIDEO_URI = stringPreferencesKey("videoUri")
    val IMAGE_URI = stringPreferencesKey("imageUri")
    val LOOP_VIDEO = booleanPreferencesKey("loopVideo")
    val TRIGGER_TYPE = stringPreferencesKey("triggerType")
    val DEBOUNCE_MS = intPreferencesKey("debounceMs")
    val MIN_ACTIVE_MS = intPreferencesKey("minActiveMs")
    val MIN_IDLE_MS = intPreferencesKey("minIdleMs")
    val AUTOSTART = booleanPreferencesKey("autostart")
    val KIOSK = booleanPreferencesKey("kiosk")
    val BLE_SERVICE_UUID = stringPreferencesKey("bleServiceUuid")
    val BLE_CHAR_UUID = stringPreferencesKey("bleCharUuid")
    val DIAGNOSTIC = booleanPreferencesKey("diagnostic")
    val HTTP_PORT = intPreferencesKey("httpPort")

    suspend fun readAll(ctx: Context): Settings = ctx.dataStore.data.map { p ->
        Settings(
            videoUri = p[VIDEO_URI],
            imageUri = p[IMAGE_URI],
            loopVideo = p[LOOP_VIDEO] ?: true,
            trigger = p[TRIGGER_TYPE]?.let { runCatching { TriggerType.valueOf(it) }.getOrNull() } ?: TriggerType.HTTP,
            debounceMs = p[DEBOUNCE_MS] ?: 200,
            minActiveMs = p[MIN_ACTIVE_MS] ?: 1000,
            minIdleMs = p[MIN_IDLE_MS] ?: 1000,
            autostart = p[AUTOSTART] ?: true,
            kiosk = p[KIOSK] ?: false,
            bleServiceUuid = p[BLE_SERVICE_UUID] ?: "",
            bleCharUuid = p[BLE_CHAR_UUID] ?: "",
            diagnostic = p[DIAGNOSTIC] ?: false,
            httpPort = p[HTTP_PORT] ?: BuildConfig.DEFAULT_HTTP_PORT
        )
    }.first()

    suspend fun write(ctx: Context, block: MutablePreferences.() -> Unit) {
        ctx.dataStore.edit { block(it) }
    }
}

data class Settings(
    val videoUri: String?,
    val imageUri: String?,
    val loopVideo: Boolean,
    val trigger: TriggerType,
    val debounceMs: Int,
    val minActiveMs: Int,
    val minIdleMs: Int,
    val autostart: Boolean,
    val kiosk: Boolean,
    val bleServiceUuid: String,
    val bleCharUuid: String,
    val diagnostic: Boolean,
    val httpPort: Int
)
