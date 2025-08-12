package id.hirejob.kiosk.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Simpan DataStore di file "kiosk_prefs"
private val Context.dataStore by preferencesDataStore(name = "kiosk_prefs")

enum class TriggerType { USB_HID, BT_HID, HEADSET, HTTP, BLE_GATT, VOLUME }

object Prefs {
    // ---------- NAMA KEY (String) ----------
    const val K_VIDEO_URI        = "video_uri"
    const val K_IMAGE_URI        = "image_uri"
    const val K_TRIGGER_SOURCE   = "trigger_source"
    const val K_BLE_SERVICE_UUID = "ble_service_uuid"
    const val K_BLE_CHAR_UUID    = "ble_char_uuid"

    const val K_LOOP_VIDEO   = "loop_video"
    const val K_AUTOSTART    = "autostart"
    const val K_KIOSK_MODE   = "kiosk_mode"
    const val K_DIAGNOSTIC   = "diagnostic"

    const val K_DEBOUNCE_MS  = "debounce_ms"
    const val K_MIN_ACTIVE_MS= "min_active_ms"
    const val K_MIN_IDLE_MS  = "min_idle_ms"

    const val K_HTTP_PORT_STR= "httpPortStr"
    const val K_HTTP_PORT    = "httpPort"

    // ---------- Preferences.Key ----------
    val VIDEO_URI        = stringPreferencesKey(K_VIDEO_URI)
    val IMAGE_URI        = stringPreferencesKey(K_IMAGE_URI)
    val TRIGGER_SOURCE   = stringPreferencesKey(K_TRIGGER_SOURCE)
    val BLE_SERVICE_UUID = stringPreferencesKey(K_BLE_SERVICE_UUID)
    val BLE_CHAR_UUID    = stringPreferencesKey(K_BLE_CHAR_UUID)

    val LOOP_VIDEO   = booleanPreferencesKey(K_LOOP_VIDEO)
    val AUTOSTART    = booleanPreferencesKey(K_AUTOSTART)
    val KIOSK_MODE   = booleanPreferencesKey(K_KIOSK_MODE)
    val DIAGNOSTIC   = booleanPreferencesKey(K_DIAGNOSTIC)

    val DEBOUNCE_MS  = intPreferencesKey(K_DEBOUNCE_MS)
    val MIN_ACTIVE_MS= intPreferencesKey(K_MIN_ACTIVE_MS)
    val MIN_IDLE_MS  = intPreferencesKey(K_MIN_IDLE_MS)

    val HTTP_PORT_STR= stringPreferencesKey(K_HTTP_PORT_STR)
    val HTTP_PORT    = intPreferencesKey(K_HTTP_PORT)

    /** Baca semua setting dengan default aman + migrasi port */
    suspend fun readAll(ctx: Context): Settings =
        ctx.dataStore.data.map { p ->
            val triggerName = p[TRIGGER_SOURCE] ?: TriggerType.HTTP.name
            val triggerEnum = runCatching { TriggerType.valueOf(triggerName) }.getOrElse { TriggerType.HTTP }

            val portFromInt  = p[HTTP_PORT]
            val portFromStr  = p[HTTP_PORT_STR]?.toIntOrNull()
            val httpPort     = portFromInt ?: portFromStr ?: HttpConstants.DEFAULT_HTTP_PORT
            val httpPortStr  = p[HTTP_PORT_STR] ?: httpPort.toString()

            Settings(
                videoUri      = p[VIDEO_URI],
                imageUri      = p[IMAGE_URI],
                loopVideo     = p[LOOP_VIDEO] ?: true,
                trigger       = triggerEnum,
                debounceMs    = p[DEBOUNCE_MS] ?: 200,
                minActiveMs   = p[MIN_ACTIVE_MS] ?: 1000,
                minIdleMs     = p[MIN_IDLE_MS] ?: 1000,
                autostart     = p[AUTOSTART] ?: true,
                kiosk         = p[KIOSK_MODE] ?: false,
                bleServiceUuid= p[BLE_SERVICE_UUID] ?: "",
                bleCharUuid   = p[BLE_CHAR_UUID] ?: "",
                diagnostic    = p[DIAGNOSTIC] ?: false,
                httpPort      = httpPort,
                httpPortStr   = httpPortStr
            )
        }.first()

    /** Tulis ke DataStore */
    suspend fun write(ctx: Context, block: MutablePreferences.() -> Unit) {
        ctx.dataStore.edit { block(it) }
    }

    /** Simpan semua field Settings */
    suspend fun saveSettings(ctx: Context, s: Settings) = write(ctx) {
        s.videoUri?.let { this[VIDEO_URI] = it } ?: remove(VIDEO_URI)
        s.imageUri?.let { this[IMAGE_URI] = it } ?: remove(IMAGE_URI)
        this[LOOP_VIDEO]       = s.loopVideo
        this[TRIGGER_SOURCE]   = s.trigger.name
        this[DEBOUNCE_MS]      = s.debounceMs
        this[MIN_ACTIVE_MS]    = s.minActiveMs
        this[MIN_IDLE_MS]      = s.minIdleMs
        this[AUTOSTART]        = s.autostart
        this[KIOSK_MODE]       = s.kiosk
        this[BLE_SERVICE_UUID] = s.bleServiceUuid
        this[BLE_CHAR_UUID]    = s.bleCharUuid
        this[DIAGNOSTIC]       = s.diagnostic
        this[HTTP_PORT]        = s.httpPort
        this[HTTP_PORT_STR]    = s.httpPortStr
    }

    // ===== PIN (pakai SharedPreferences biar simple & synchronous)
    private const val FILE   = "kiosk_prefs"
    private const val K_PIN  = "admin_pin"
    private const val DEFAULT_PIN = "2480" // default

    fun pin(ctx: Context): String =
        ctx.getSharedPreferences(FILE, 0).getString(K_PIN, DEFAULT_PIN)!!

    fun isPinValid(pin: String) = pin.length in 4..8 && pin.all { it.isDigit() }

    /** return true kalau berhasil disimpan (valid), false kalau formatnya salah */
    fun setPin(ctx: Context, value: String): Boolean {
        if (!isPinValid(value)) return false
        ctx.getSharedPreferences(FILE, 0).edit().putString(K_PIN, value).apply()
        return true
    }

    private fun sKey(name: String) = stringPreferencesKey(name)
    private fun bKey(name: String) = booleanPreferencesKey(name)
    private fun iKey(name: String) = intPreferencesKey(name)

    // ===== Helper per-field (Flow)
    fun videoUri(ctx: Context)        = ctx.dataStore.data.map { it[VIDEO_URI] ?: "" }
    suspend fun setVideoUri(ctx: Context, v: String) = ctx.dataStore.edit { it[VIDEO_URI] = v }

    fun imageUri(ctx: Context)        = ctx.dataStore.data.map { it[IMAGE_URI] ?: "" }
    suspend fun setImageUri(ctx: Context, v: String) = ctx.dataStore.edit { it[IMAGE_URI] = v }

    fun triggerSource(ctx: Context)   = ctx.dataStore.data.map { it[TRIGGER_SOURCE] ?: TriggerType.HTTP.name }
    suspend fun setTriggerSource(ctx: Context, v: String) = ctx.dataStore.edit { it[TRIGGER_SOURCE] = v }

    fun loopVideo(ctx: Context)       = ctx.dataStore.data.map { it[LOOP_VIDEO] ?: true }
    suspend fun setLoopVideo(ctx: Context, v: Boolean) = ctx.dataStore.edit { it[LOOP_VIDEO] = v }

    fun autostart(ctx: Context)       = ctx.dataStore.data.map { it[AUTOSTART] ?: true }
    suspend fun setAutostart(ctx: Context, v: Boolean) = ctx.dataStore.edit { it[AUTOSTART] = v }

    fun kioskMode(ctx: Context)       = ctx.dataStore.data.map { it[KIOSK_MODE] ?: false }
    suspend fun setKioskMode(ctx: Context, v: Boolean) = ctx.dataStore.edit { it[KIOSK_MODE] = v }

    fun debounceMs(ctx: Context)      = ctx.dataStore.data.map { it[DEBOUNCE_MS] ?: 200 }
    suspend fun setDebounceMs(ctx: Context, v: Int) = ctx.dataStore.edit { it[DEBOUNCE_MS] = v }

    fun minActiveMs(ctx: Context)     = ctx.dataStore.data.map { it[MIN_ACTIVE_MS] ?: 1000 }
    suspend fun setMinActiveMs(ctx: Context, v: Int) = ctx.dataStore.edit { it[MIN_ACTIVE_MS] = v }

    fun minIdleMs(ctx: Context)       = ctx.dataStore.data.map { it[MIN_IDLE_MS] ?: 1000 }
    suspend fun setMinIdleMs(ctx: Context, v: Int) = ctx.dataStore.edit { it[MIN_IDLE_MS] = v }

    fun diagnostic(ctx: Context)      = ctx.dataStore.data.map { it[DIAGNOSTIC] ?: false }
    suspend fun setDiagnostic(ctx: Context, v: Boolean) = ctx.dataStore.edit { it[DIAGNOSTIC] = v }

    fun bleServiceUuid(ctx: Context)  = ctx.dataStore.data.map { it[BLE_SERVICE_UUID] ?: "" }
    suspend fun setBleServiceUuid(ctx: Context, v: String) = ctx.dataStore.edit { it[BLE_SERVICE_UUID] = v }

    fun bleCharUuid(ctx: Context)     = ctx.dataStore.data.map { it[BLE_CHAR_UUID] ?: "" }
    suspend fun setBleCharUuid(ctx: Context, v: String) = ctx.dataStore.edit { it[BLE_CHAR_UUID] = v }
    
    fun httpPort(ctx: Context)        = ctx.dataStore.data.map { it[HTTP_PORT] ?: HttpConstants.DEFAULT_HTTP_PORT }
    suspend fun setHttpPort(ctx: Context, v: Int) = ctx.dataStore.edit { it[HTTP_PORT] = v }
}
data class Settings(
    val videoUri: String? = null,
    val imageUri: String? = null,
    val loopVideo: Boolean = true,
    val trigger: TriggerType = TriggerType.HTTP,
    val debounceMs: Int = 200,
    val minActiveMs: Int = 1000,
    val minIdleMs: Int = 1000,
    val autostart: Boolean = true,
    val kiosk: Boolean = false,
    val bleServiceUuid: String = "",
    val bleCharUuid: String = "",
    val diagnostic: Boolean = false,
    val httpPort: Int = HttpConstants.DEFAULT_HTTP_PORT,
    val httpPortStr: String = HttpConstants.DEFAULT_HTTP_PORT.toString()
)