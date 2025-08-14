// app/src/main/java/id/hirejob/kiosk/settings/SettingsFragment.kt
package id.hirejob.kiosk.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.os.Bundle
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.lifecycle.lifecycleScope
import id.hirejob.kiosk.R
import id.hirejob.kiosk.core.Prefs
import kotlinx.coroutines.launch


class SettingsFragment : PreferenceFragmentCompat() {

    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) { /* beberapa emulator bisa abaikan */ }
        viewLifecycleOwner.lifecycleScope.launch {
            Prefs.setVideoUri(requireContext(), uri.toString())
            findPreference<Preference>(Prefs.K_VIDEO_URI)?.summary = uri.toString()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) { }
        viewLifecycleOwner.lifecycleScope.launch {
            Prefs.setImageUri(requireContext(), uri.toString())
            findPreference<Preference>(Prefs.K_IMAGE_URI)?.summary = uri.toString()
        }
    }

    private fun validateUsbHidKey(input: String): Boolean {
        if (input.isBlank()) return true // biarkan, nanti default di model
        val s = input.uppercase()
        if (s.matches(Regex("^F(1[0-2]|[1-9])$"))) return true
        return input.trim().length == 1
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // bridge ke DataStore
        preferenceManager.preferenceDataStore = DsStore(requireContext().applicationContext)
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>(Prefs.K_VIDEO_URI)?.setOnPreferenceClickListener {
            pickVideo.launch(arrayOf("video/*"))
            true
        }

        findPreference<Preference>("image_uri")?.setOnPreferenceClickListener {
            // dukung GIF juga
            pickImage.launch(arrayOf("image/*"))
            true
        }

        // angka only
        findPreference<EditTextPreference>(Prefs.K_DEBOUNCE_MS)
            ?.setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
        findPreference<EditTextPreference>(Prefs.K_MIN_ACTIVE_MS)
            ?.setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
        findPreference<EditTextPreference>(Prefs.K_MIN_IDLE_MS)
            ?.setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }

        // summary otomatis
        findPreference<ListPreference>(Prefs.K_TRIGGER_SOURCE)
            ?.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance())

        // USB HID key
        val usb = findPreference<EditTextPreference>("usb_hid_key")

        usb?.setOnBindEditTextListener { et ->
            et.maxLines = 1
            et.isSingleLine = true
        }

        usb?.setOnPreferenceChangeListener { _, new ->
            val raw = (new as? String)?.trim().orEmpty()
            val ok = validateUsbHidKey(raw)
            if (!ok) {
                Toast.makeText(requireContext(),
                    "Isi F1..F12 atau tepat 1 karakter.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            ok
        }

        // tampilkan ringkasannya rapi (F9 atau char)
        usb?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { pref ->
            val v = (pref.text ?: "F9").trim()
            "Saat ini: ${v.ifBlank { "F9" }}"
        }
    }
}
