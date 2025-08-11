package id.hirejob.kiosk.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build

object KioskHelper {
    fun tryStartLockTask(activity: Activity) {
        try { activity.startLockTask() } catch (_: Throwable) { /* screen pinning fallback */ }
    }

    fun isDeviceOwner(ctx: Context): Boolean {
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return if (Build.VERSION.SDK_INT >= 24) dpm.isDeviceOwnerApp(ctx.packageName) else false
    }
}
