package id.hirejob.kiosk.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import id.hirejob.kiosk.R
import id.hirejob.kiosk.core.Prefs
import id.hirejob.kiosk.ui.stopKioskModeIfAny
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DsStore(private val context: Context) : PreferenceDataStore() {
    override fun putString(
        key: String,
        value: String?,
    ) {
        lifecycleScopeOrBlocking {
            when (key) {
                Prefs.K_USB_HID -> Prefs.setUsbHidKey(context, (value ?: "F9").trim())
                Prefs.K_VIDEO_URI -> Prefs.setVideoUri(context, value.orEmpty())
                Prefs.K_IMAGE_URI -> Prefs.setImageUri(context, value.orEmpty())
                Prefs.K_TRIGGER_SOURCE -> Prefs.setTriggerSource(context, value.orEmpty())
                Prefs.K_BLE_SERVICE_UUID -> Prefs.setBleServiceUuid(context, value.orEmpty())
                Prefs.K_BLE_CHAR_UUID -> Prefs.setBleCharUuid(context, value.orEmpty())
            }
        }
    }

    override fun getString(
        key: String,
        defValue: String?,
    ): String =
        runBlocking {
            when (key) {
                Prefs.K_USB_HID -> Prefs.usbHidKey(context).first()
                Prefs.K_VIDEO_URI -> Prefs.videoUri(context).first()
                Prefs.K_IMAGE_URI -> Prefs.imageUri(context).first()
                Prefs.K_TRIGGER_SOURCE -> Prefs.triggerSource(context).first()
                Prefs.K_BLE_SERVICE_UUID -> Prefs.bleServiceUuid(context).first()
                Prefs.K_BLE_CHAR_UUID -> Prefs.bleCharUuid(context).first()
                else -> defValue.orEmpty()
            }
        }

    override fun putBoolean(
        key: String,
        value: Boolean,
    ) = lifecycleScopeOrBlocking {
        when (key) {
            Prefs.K_LOOP_VIDEO -> Prefs.setLoopVideo(context, value)
            Prefs.K_AUTOSTART -> Prefs.setAutostart(context, value)
            Prefs.K_KIOSK_MODE -> {
                Prefs.setKioskMode(context, value)
                toggleHomeComponent(context, value)

                // optional: paksa user pilih launcher sekarang
                if (value) {
                    // kirim HOME supaya chooser muncul; user pilih HireKiosk -> Always
                    val i =
                        Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    context.startActivity(i)
                }
            }
            Prefs.K_DIAGNOSTIC -> Prefs.setDiagnostic(context, value)
            Prefs.K_POWER_INVERT -> Prefs.setPowerInvert(context, value)
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
    }

    override fun getBoolean(
        key: String,
        defValue: Boolean,
    ): Boolean =
        runBlocking {
            when (key) {
                Prefs.K_LOOP_VIDEO -> Prefs.loopVideo(context).first()
                Prefs.K_AUTOSTART -> Prefs.autostart(context).first()
                Prefs.K_KIOSK_MODE -> Prefs.kioskMode(context).first()
                Prefs.K_DIAGNOSTIC -> Prefs.diagnostic(context).first()
                Prefs.K_POWER_INVERT -> Prefs.powerInvert(context).first()
                else -> defValue
            }
        }

    override fun putInt(
        key: String,
        value: Int,
    ) = lifecycleScopeOrBlocking {
        when (key) {
            Prefs.K_DEBOUNCE_MS -> Prefs.setDebounceMs(context, value)
            Prefs.K_MIN_ACTIVE_MS -> Prefs.setMinActiveMs(context, value)
            Prefs.K_MIN_IDLE_MS -> Prefs.setMinIdleMs(context, value)
        }
    }

    override fun getInt(
        key: String,
        defValue: Int,
    ): Int =
        runBlocking {
            when (key) {
                Prefs.K_DEBOUNCE_MS -> Prefs.debounceMs(context).first()
                Prefs.K_MIN_ACTIVE_MS -> Prefs.minActiveMs(context).first()
                Prefs.K_MIN_IDLE_MS -> Prefs.minIdleMs(context).first()
                else -> defValue
            }
        }

    private inline fun lifecycleScopeOrBlocking(crossinline block: suspend () -> Unit) {
        (context as? AppCompatActivity)?.lifecycleScope?.launch { block() } ?: runBlocking { block() }
    }

    private fun toggleHomeComponent(
        context: Context,
        enable: Boolean,
    ) {
        val pm = context.packageManager
        val comp = ComponentName(context, "id.hirejob.kiosk.ui.KioskHomeActivity")
        pm.setComponentEnabledSetting(
            comp,
            if (enable) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP,
        )
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
                p1 != p2 -> toast("PIN tidak sama")
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
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun Context.toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onResume() {
        super.onResume()
        stopKioskModeIfAny()
    }
}
