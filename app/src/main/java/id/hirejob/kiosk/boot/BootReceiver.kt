package id.hirejob.kiosk.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.hirejob.kiosk.core.Prefs
import id.hirejob.kiosk.core.KioskService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.Default).launch {
            val s = Prefs.readAll(context)
            if (s.autostart) {
                context.startForegroundService(Intent(context, KioskService::class.java))
            }
        }
    }
}
