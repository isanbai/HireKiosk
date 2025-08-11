package id.hirejob.kiosk.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import id.hirejob.kiosk.R
import id.hirejob.kiosk.trigger.HttpTrigger
import id.hirejob.kiosk.core.HttpConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class KioskService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var http: HttpTrigger? = null
    private var sm: StateMachine? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(5, notif("Starting…"))
        scope.launch {
            val settings = Prefs.readAll(this@KioskService)
            val triggerFlow: Flow<Boolean> = when (settings.trigger) {
                TriggerType.HTTP -> {
                    http = HttpTrigger(settings.httpPortStr).also { it.start() }
                    http!!.isOn
                }
                else -> MutableStateFlow(false) // other triggers managed in activity for now
            }
            sm = StateMachine(
                scope = scope,
                triggerFlow = triggerFlow,
                debounceMs = settings.debounceMs.toLong(),
                minActiveMs = settings.minActiveMs.toLong(),
                minIdleMs = settings.minIdleMs.toLong()
            )
            sm!!.state.onEach { s ->
                val text = when (s) {
                    UiState.IDLE -> "IDLE"
                    UiState.ACTIVE -> "ACTIVE"
                }
                notifyText("State: $text  (HTTP:${settings.httpPort})")
            }.launchIn(scope)
        }
    }

    private fun channelId(): String = "kiosk"
    private fun notif(text: String): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(channelId(), "Kiosk", NotificationManager.IMPORTANCE_MIN))
        }
        return NotificationCompat.Builder(this, channelId())
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .build()
    }
    private fun notifyText(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(5, notif(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        http?.stop(); http = null
        scope.cancel()
    }
}
