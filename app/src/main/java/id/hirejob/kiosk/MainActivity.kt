package id.hirejob.kiosk

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import id.hirejob.kiosk.settings.SettingsActivity
import androidx.appcompat.widget.Toolbar
import id.hirejob.kiosk.core.SecretGate
import android.view.WindowManager
import id.hirejob.kiosk.ui.*
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import id.hirejob.kiosk.device.KioskPolicy
import id.hirejob.kiosk.core.KioskWatcher

class MainActivity : AppCompatActivity() {

    private lateinit var gate: SecretGate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startForegroundService(Intent(this, KioskWatcher::class.java))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableImmersive()
        supportActionBar?.hide()

        val root = findViewById<View>(android.R.id.content)   // seluruh layar
        gate = SecretGate(
            hostActivity = this,
            onUnlocked = { startActivity(Intent(this, SettingsActivity::class.java)) },
            enableTripleTap = true,
            enableLongPress = true,
            cornerOnlyDp = 48  // gesture hanya di area 48dp kiri-atas; hapus kalau mau seluruh layar
        )
        gate.attachTo(root)

        applyKioskUi()

        KioskPolicy.apply(this)
    }

    override fun onResume() {
        super.onResume()
        // Re-apply kalau user sempat munculin bar lewat gesture
        enableImmersive()
        hideSystemBars()
        // maybeStartLockTask()
        startKioskModeIfPossible()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersive() 
        hideSystemBars()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun maybeStartLockTask() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val permitted = if (android.os.Build.VERSION.SDK_INT >= 23)
            dpm.isLockTaskPermitted(packageName) else true

        if (!isInLockTaskMode()) {
            try {
                // kalau belum device owner, ini akan munculin dialog "Start screen pinning"
                startLockTask()
            } catch (_: IllegalArgumentException) { /* ignore */ }
        }
    }

    private fun isInLockTaskMode(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (android.os.Build.VERSION.SDK_INT >= 23)
            am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        else @Suppress("DEPRECATION") am.isInLockTaskMode
    }
}
