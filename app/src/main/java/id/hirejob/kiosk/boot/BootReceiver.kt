package id.hirejob.kiosk.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.hirejob.kiosk.core.ensureKioskService
import id.hirejob.kiosk.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start service
            ctx.ensureKioskService()

            // (opsional) buka UI utama
            val ui = Intent(ctx, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(ui)
        }
    }
}
