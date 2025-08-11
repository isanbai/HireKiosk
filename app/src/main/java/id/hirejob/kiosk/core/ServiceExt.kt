package id.hirejob.kiosk.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

fun Context.ensureKioskService() {
    ContextCompat.startForegroundService(
        this,
        Intent(this, KioskService::class.java)
    )
}
