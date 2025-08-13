// app/src/main/java/id/hirejob/kiosk/settings/SettingsFragment.kt
package id.hirejob.kiosk.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
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

        // hint text
        // findPreference<EditTextPreference>(Prefs.K_VIDEO_URI)
        //     ?.setOnBindEditTextListener { it.hint = "Path or URI to the video file" }
        // findPreference<EditTextPreference>(Prefs.K_IMAGE_URI)
        //     ?.setOnBindEditTextListener { it.hint = "Path or URI to the image/GIF file" }

        // summary otomatis
        findPreference<ListPreference>(Prefs.K_TRIGGER_SOURCE)
            ?.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance())
    }
}

// class SettingsFragment : PreferenceFragmentCompat() {

//     private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
//         uri ?: return@registerForActivityResult
//         try {
//             requireContext().contentResolver.takePersistableUriPermission(
//                 uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
//             )
//         } catch (_: SecurityException) { /* beberapa emulator bisa abaikan */ }

//         viewLifecycleOwner.lifecycleScope.launch {
//             Prefs.setVideoUri(requireContext(), uri.toString())
//             findPreference<Preference>("video_uri")?.summary = uri.toString()
//         }
//     }

//     override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//         setPreferencesFromResource(R.xml.preferences, rootKey)

//         // Tampilkan nilai saat ini
//         viewLifecycleOwner.lifecycleScope.launch {
//             val s = Prefs.readAll(requireContext())
//             findPreference<Preference>("video_uri")?.summary = s.videoUri ?: ""
//         }

//         // Klik "Video" => buka SAF
//         findPreference<Preference>("video_uri")?.setOnPreferenceClickListener {
//             pickVideo.launch(arrayOf("video/*"))
//             true
//         }

//         // Klik "Reset Video" => kembali ke resource default
//         findPreference<Preference>("reset_video")?.setOnPreferenceClickListener {
//             val def = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.anim}")
//             viewLifecycleOwner.lifecycleScope.launch {
//                 Prefs.setVideoUri(requireContext(), def.toString())
//                 findPreference<Preference>("video_uri")?.summary = def.toString()
//             }
//             true
//         }
//     }
// }