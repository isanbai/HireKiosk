package id.hirejob.kiosk.device

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

object KioskPolicy {
    fun apply(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AdminReceiver::class.java)
        if (dpm.isDeviceOwnerApp(context.packageName)) {
            // izinkan app ini pakai LockTask tanpa prompt
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
            if (Build.VERSION.SDK_INT >= 28) dpm.setKeyguardDisabled(admin, true)
        }
    }
}
