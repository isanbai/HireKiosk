package id.hirejob.kiosk.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Launcher stub khusus HOME. Tidak punya UI;
 * hanya melempar ke MainActivity lalu selesai.
 * Di-enable hanya saat Kiosk Mode = ON.
 */
class KioskHomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }
}
