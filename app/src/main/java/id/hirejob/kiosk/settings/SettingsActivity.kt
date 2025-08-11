package id.hirejob.kiosk.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import android.view.View
import android.widget.EditText
import id.hirejob.kiosk.core.Prefs
import android.content.Context
import android.widget.Toast
import id.hirejob.kiosk.R
import id.hirejob.kiosk.ui.stopKioskModeIfAny

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) pasang layout dulu
        setContentView(R.layout.activity_settings)

        // 2) tempel PreferenceFragment ke container milik layout
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }

        // 3) sekarang aman panggil findViewById
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
    }

    private fun Context.toast(s: String) =
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }

    override fun onResume() {
        super.onResume()
        stopKioskModeIfAny()
    }
}
