package id.hirejob.kiosk.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.hirejob.kiosk.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        GlobalScope.launch(Dispatchers.Default) {
            delay(2500)
            val a = Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ctx.startActivity(a)
        }
    }
}
