package id.hirejob.kiosk.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
/**
 * Ensure that the KioskService is running.
 * This is typically called after boot or when the app is launched.
 */

fun Context.ensureKioskService() {
    ContextCompat.startForegroundService(
        this,
        Intent(this, KioskService::class.java)
    )
}
