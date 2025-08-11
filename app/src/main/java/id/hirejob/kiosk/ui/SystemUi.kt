// ui/SystemUi.kt
package id.hirejob.kiosk.ui

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.enableImmersive() {
    // Biar content nembus ke full screen
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
}
