package id.hirejob.kiosk.core

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Simpan DataStore di file "kiosk_prefs"
private val Context.dataStore by preferencesDataStore(name = "kiosk_prefs")

enum class TriggerType { USB_HID, BT_HID, HEADSET, HTTP, BLE_GATT, VOLUME }

object Prefs {
    // Keys
    val VIDEO_URI       = stringPreferencesKey("videoUri")
    val IMAGE_URI       = stringPreferencesKey("imageUri")
    val LOOP_VIDEO      = booleanPreferencesKey("loopVideo")
    val TRIGGER_TYPE    = stringPreferencesKey("triggerType")
    val DEBOUNCE_MS     = intPreferencesKey("debounceMs")
    val MIN_ACTIVE_MS   = intPreferencesKey("minActiveMs")
    val MIN_IDLE_MS     = intPreferencesKey("minIdleMs")
    val AUTOSTART       = booleanPreferencesKey("autostart")
    val KIOSK           = booleanPreferencesKey("kiosk")
    val BLE_SERVICE_UUID= stringPreferencesKey("bleServiceUuid")
    val BLE_CHAR_UUID   = stringPreferencesKey("bleCharUuid")
    val DIAGNOSTIC      = booleanPreferencesKey("diagnostic")

    // Port: jaga kompatibilitas lama (string) + baru (int)
    val HTTP_PORT_STR   = stringPreferencesKey("httpPortStr")
    val HTTP_PORT       = intPreferencesKey("httpPort")

    /** Baca semua setting dengan default yang aman + migrasi port */
    suspend fun readAll(ctx: Context): Settings = ctx.dataStore.data.map { p ->
        val trigger = p[TRIGGER_TYPE]
            ?.let { runCatching { TriggerType.valueOf(it) }.getOrNull() }
            ?: TriggerType.HTTP

        // Prefer int; kalau kosong, coba parse dari string; kalau gagal pakai default
        val portFromInt  = p[HTTP_PORT]
        val portFromStr  = p[HTTP_PORT_STR]?.toIntOrNull()
        val httpPort     = portFromInt ?: portFromStr ?: HttpConstants.DEFAULT_HTTP_PORT
        val httpPortStr  = p[HTTP_PORT_STR] ?: httpPort.toString()

        Settings(
            videoUri      = p[VIDEO_URI],
            imageUri      = p[IMAGE_URI],
            loopVideo     = p[LOOP_VIDEO] ?: true,
            trigger       = trigger,
            debounceMs    = p[DEBOUNCE_MS] ?: 200,
            minActiveMs   = p[MIN_ACTIVE_MS] ?: 1000,
            minIdleMs     = p[MIN_IDLE_MS] ?: 1000,
            autostart     = p[AUTOSTART] ?: true,
            kiosk         = p[KIOSK] ?: false,
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

    /** Helper untuk menyimpan semua field Settings sekaligus */
    suspend fun saveSettings(ctx: Context, s: Settings) = write(ctx) {
        s.videoUri?.let { this[VIDEO_URI] = it } ?: remove(VIDEO_URI)
        s.imageUri?.let { this[IMAGE_URI] = it } ?: remove(IMAGE_URI)
        this[LOOP_VIDEO]       = s.loopVideo
        this[TRIGGER_TYPE]     = s.trigger.name
        this[DEBOUNCE_MS]      = s.debounceMs
        this[MIN_ACTIVE_MS]    = s.minActiveMs
        this[MIN_IDLE_MS]      = s.minIdleMs
        this[AUTOSTART]        = s.autostart
        this[KIOSK]            = s.kiosk
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
    val httpPort: Int,
    val httpPortStr: String
)
