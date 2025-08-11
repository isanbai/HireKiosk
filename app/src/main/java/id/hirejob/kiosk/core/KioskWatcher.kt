// KioskWatcher.kt
package id.hirejob.kiosk.core

import kotlinx.coroutines.*
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import id.hirejob.kiosk.ui.MainActivity
import id.hirejob.kiosk.R
import id.hirejob.kiosk.core.Prefs
import id.hirejob.kiosk.core.TriggerType
import id.hirejob.kiosk.core.StateMachine
import id.hirejob.kiosk.core.UiState

class KioskWatcher : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startForeground(7, notif("Kiosk berjalan"))
        scope.launch {
            while (isActive) {
                delay(3000)
                if (!isInLockTask()) {
                    val a = Intent(this@KioskWatcher, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(a)
                }
            }
        }
    }
    override fun onBind(i: Intent?) = null

    private fun isInLockTask(): Boolean {
        val am = getSystemService(ActivityManager::class.java)
        return if (Build.VERSION.SDK_INT >= 23)
            am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        else @Suppress("DEPRECATION") am.isInLockTaskMode
    }

    private fun notif(text: String): Notification {
        val chId = "kiosk"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 &&
            nm.getNotificationChannel(chId) == null) {
            nm.createNotificationChannel(NotificationChannel(chId, "Kiosk", NotificationManager.IMPORTANCE_MIN))
        }
        return NotificationCompat.Builder(this, chId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("HireKiosk")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }
}
