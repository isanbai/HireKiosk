package id.hirejob.kiosk.ui

import android.content.Intent
import android.net.Uri
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import id.hirejob.kiosk.core.Prefs
import id.hirejob.kiosk.core.TriggerType
import id.hirejob.kiosk.R
import kotlinx.coroutines.*
import androidx.datastore.preferences.core.edit

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFrag())
            .commit()
    }

    class SettingsFrag : PreferenceFragmentCompat() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) scope.launch { Prefs.write(requireContext()) { this[Prefs.VIDEO_URI] = uri.toString() } }
        }
        private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) scope.launch { Prefs.write(requireContext()) { this[Prefs.IMAGE_URI] = uri.toString() } }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = null
            setPreferencesFromResource(R.xml.prefs, rootKey)

            findPreference<Preference>("videoPick")?.setOnPreferenceClickListener {
                pickVideo.launch(arrayOf("video/*")); true
            }
            findPreference<Preference>("imagePick")?.setOnPreferenceClickListener {
                pickImage.launch(arrayOf("image/*", "image/gif")); true
            }

            // Loop
            bindBool("loopVideo", Prefs.LOOP_VIDEO)
            bindBool("autostart", Prefs.AUTOSTART)
            bindBool("kiosk", Prefs.KIOSK)
            bindBool("diagnostic", Prefs.DIAGNOSTIC)

            bindInt("debounceMs", Prefs.DEBOUNCE_MS)
            bindInt("minActiveMs", Prefs.MIN_ACTIVE_MS)
            bindInt("minIdleMs", Prefs.MIN_IDLE_MS)
            bindInt("httpPort", Prefs.HTTP_PORT)

            // Trigger
            val list = findPreference<ListPreference>("triggerType")
            list?.entries = TriggerType.entries.map { it.name }.toTypedArray()
            list?.entryValues = list.entries
            list?.setOnPreferenceChangeListener { _, newVal ->
                scope.launch { Prefs.write(requireContext()) { this[Prefs.TRIGGER_TYPE] = newVal.toString() } }
                true
            }

            // Wizard (stub for now)
            findPreference<Preference>("wizard")?.setOnPreferenceClickListener {
                // simple flow: enable autostart, suggest pin
                scope.launch {
                    Prefs.write(requireContext()) {
                        this[Prefs.AUTOSTART] = true
                        this[Prefs.KIOSK] = true
                    }
                }
                true
            }

            // Start/stop service buttons
            findPreference<Preference>("startService")?.setOnPreferenceClickListener {
                requireContext().startForegroundService(Intent(requireContext(), id.hirejob.kiosk.core.KioskService::class.java))
                true
            }
            findPreference<Preference>("stopService")?.setOnPreferenceClickListener {
                requireContext().stopService(Intent(requireContext(), id.hirejob.kiosk.core.KioskService::class.java))
                true
            }
        }

        private fun openWizard() {
            startActivity(Intent(this@SettingsActivity, 
                id.hirejob.kiosk.settings.WizardKioskActivity::class.java))
        }

        private fun bindBool(keyPref: String, keyStore: androidx.datastore.preferences.core.Preferences.Key<Boolean>) {
            (findPreference<SwitchPreferenceCompat>(keyPref))?.setOnPreferenceChangeListener { _, newVal ->
                scope.launch { Prefs.write(requireContext()) { this[keyStore] = newVal as Boolean } }
                true
            }
        }
        private fun bindInt(keyPref: String, keyStore: androidx.datastore.preferences.core.Preferences.Key<Int>) {
            (findPreference<EditTextPreference>(keyPref))?.setOnPreferenceChangeListener { _, newVal ->
                val v = (newVal as? String)?.toIntOrNull() ?: return@setOnPreferenceChangeListener false
                scope.launch { Prefs.write(requireContext()) { this[keyStore] = v } }
                true
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            scope.cancel()
        }
    }
}
