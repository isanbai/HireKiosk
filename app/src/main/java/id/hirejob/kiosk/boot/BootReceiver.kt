package id.hirejob.kiosk.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.hirejob.kiosk.ui.MainActivity
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        ctx: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Respect user setting: only autostart when enabled
            val autostart = runBlocking { id.hirejob.kiosk.core.Prefs.readAll(ctx).autostart }
            if (!autostart) return
            // Start http service
            // ctx.ensureKioskService()

            // (opsional) buka UI utama
            val ui =
                Intent(ctx, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            ctx.startActivity(ui)
        }
    }
}
