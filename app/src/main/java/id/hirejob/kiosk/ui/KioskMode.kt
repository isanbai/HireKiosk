package id.hirejob.kiosk.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build

fun Activity.startKioskModeIfPossible() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        try {
            startLockTask()
        } catch (_: Throwable) {
            // ignore
        }
    }
}

fun Activity.stopKioskModeIfAny() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        try {
            stopLockTask()
        } catch (_: Throwable) {
            // ignore
        }
    }
}

/** Cek apakah app ini device-owner (full kiosk) */
fun Context.isDeviceOwner(): Boolean {
    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return dpm.isDeviceOwnerApp(packageName)
}

/** Boleh lock-task tanpa prompt? (kalo DO atau diizinkan admin) */
fun Context.canLockTask(): Boolean {
    return try {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dpm.isLockTaskPermitted(packageName) || dpm.isDeviceOwnerApp(packageName)
        } else {
            true
        }
    } catch (_: Throwable) {
        false
    }
}
