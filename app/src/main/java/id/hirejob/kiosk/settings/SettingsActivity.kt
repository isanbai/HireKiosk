package id.hirejob.kiosk.settings

import android.os.Bundle
import android.net.Uri
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceDataStore
import androidx.preference.ListPreference
import androidx.preference.EditTextPreference
import androidx.lifecycle.lifecycleScope
import android.view.View
import android.widget.EditText
import android.view.MenuItem
import android.text.InputType
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import id.hirejob.kiosk.core.Prefs
import android.content.Context
import android.widget.Toast
import id.hirejob.kiosk.R
import id.hirejob.kiosk.ui.stopKioskModeIfAny
import id.hirejob.kiosk.ui.startKioskModeIfPossible


import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference


class DsStore(private val context: Context) : PreferenceDataStore() {
    override fun putString(key: String, value: String?) {
        lifecycleScopeOrBlocking {
            when (key) {
                Prefs.K_VIDEO_URI        -> Prefs.setVideoUri(context, value.orEmpty())
                Prefs.K_IMAGE_URI        -> Prefs.setImageUri(context, value.orEmpty())
                Prefs.K_TRIGGER_SOURCE   -> Prefs.setTriggerSource(context, value.orEmpty())
                Prefs.K_BLE_SERVICE_UUID -> Prefs.setBleServiceUuid(context, value.orEmpty())
                Prefs.K_BLE_CHAR_UUID    -> Prefs.setBleCharUuid(context, value.orEmpty())
            }
        }
    }
    override fun getString(key: String, defValue: String?): String = runBlocking {
        when (key) {
            Prefs.K_VIDEO_URI        -> Prefs.videoUri(context).first()
            Prefs.K_IMAGE_URI        -> Prefs.imageUri(context).first()
            Prefs.K_TRIGGER_SOURCE   -> Prefs.triggerSource(context).first()
            Prefs.K_BLE_SERVICE_UUID -> Prefs.bleServiceUuid(context).first()
            Prefs.K_BLE_CHAR_UUID    -> Prefs.bleCharUuid(context).first()
            else -> defValue.orEmpty()
        }
    }

    override fun putBoolean(key: String, value: Boolean) = lifecycleScopeOrBlocking {
        when (key) {
            Prefs.K_LOOP_VIDEO -> Prefs.setLoopVideo(context, value)
            Prefs.K_AUTOSTART  -> Prefs.setAutostart(context, value)
            Prefs.K_KIOSK_MODE -> Prefs.setKioskMode(context, value)
            Prefs.K_DIAGNOSTIC -> Prefs.setDiagnostic(context, value)
        }
    }
    override fun getBoolean(key: String, defValue: Boolean): Boolean = runBlocking {
        when (key) {
            Prefs.K_LOOP_VIDEO -> Prefs.loopVideo(context).first()
            Prefs.K_AUTOSTART  -> Prefs.autostart(context).first()
            Prefs.K_KIOSK_MODE -> Prefs.kioskMode(context).first()
            Prefs.K_DIAGNOSTIC -> Prefs.diagnostic(context).first()
            else -> defValue
        }
    }

    override fun putInt(key: String, value: Int) = lifecycleScopeOrBlocking {
        when (key) {
            Prefs.K_DEBOUNCE_MS   -> Prefs.setDebounceMs(context, value)
            Prefs.K_MIN_ACTIVE_MS -> Prefs.setMinActiveMs(context, value)
            Prefs.K_MIN_IDLE_MS   -> Prefs.setMinIdleMs(context, value)
        }
    }
    override fun getInt(key: String, defValue: Int): Int = runBlocking {
        when (key) {
            Prefs.K_DEBOUNCE_MS   -> Prefs.debounceMs(context).first()
            Prefs.K_MIN_ACTIVE_MS -> Prefs.minActiveMs(context).first()
            Prefs.K_MIN_IDLE_MS   -> Prefs.minIdleMs(context).first()
            else -> defValue
        }
    }

    private inline fun lifecycleScopeOrBlocking(crossinline block: suspend () -> Unit) {
        (context as? AppCompatActivity)?.lifecycleScope?.launch { block() } ?: runBlocking { block() }
    }
}

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }

        findViewById<View>(R.id.btnSavePin).setOnClickListener {
            val p1 = findViewById<EditText>(R.id.etNewPin).text?.toString()?.trim() ?: ""
            val p2 = findViewById<EditText>(R.id.etNewPin2).text?.toString()?.trim() ?: ""
            when {
                p1.length !in 4..8 -> toast("PIN 4–8 digit")
                p1 != p2           -> toast("PIN tidak sama")
                else -> {
                    Prefs.setPin(this, p1)
                    toast("PIN disimpan")
                    findViewById<EditText>(R.id.etNewPin).text?.clear()
                    findViewById<EditText>(R.id.etNewPin2).text?.clear()
                }
            }
        }

        findViewById<View>(R.id.resetVideo).setOnClickListener {
            val vid = Uri.parse(Prefs.DEFAULT_VIDEO_URI)
            val img = Uri.parse(Prefs.DEFAULT_IMAGE_URI)
            lifecycleScope.launch {
                Prefs.setVideoUri(this@SettingsActivity, vid.toString())
                Prefs.setImageUri(this@SettingsActivity, img.toString())
            }
            toast("Media URI direset ke default")
        }
    }
        
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun Context.toast(s: String) =
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onResume() {
        super.onResume()
        stopKioskModeIfAny()
    }
}
