package id.hirejob.kiosk.device

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(c: Context, intent: Intent) {
        Toast.makeText(c, "Device owner enabled", Toast.LENGTH_SHORT).show()
    }
}
