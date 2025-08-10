package id.hirejob.kiosk

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)

    // Fullscreen sederhana (mode kiosk dasar)
    WindowCompat.getInsetsController(window, window.decorView).apply {
      hide(android.view.WindowInsets.Type.systemBars())
      systemBarsBehavior = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
  }

  override fun onBackPressed() {
    // no-op agar tidak keluar app
  }
}
