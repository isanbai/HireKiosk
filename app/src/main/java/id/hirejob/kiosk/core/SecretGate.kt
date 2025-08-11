package id.hirejob.kiosk.core

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GestureDetectorCompat
import android.view.GestureDetector
import androidx.preference.PreferenceManager
import id.hirejob.kiosk.core.Prefs

/**
 * Gerbang rahasia untuk membuka Settings via triple-tap atau long-press.
 * Saat gerbang terbuka, akan minta PIN dulu.
 */
class SecretGate(
    private val hostActivity: Activity,
    private val onUnlocked: () -> Unit,
    private val enableTripleTap: Boolean = true,
    private val enableLongPress: Boolean = true,
    private val tapsRequired: Int = 3,
    private val windowMs: Long = 1000L,     // maksimal jeda antar tap
    private val longPressMs: Long = 800L,   // durasi long-press
    private val cornerOnlyDp: Int? = null   // jika tidak null, gesture hanya valid di pojok kiri-atas dalam N dp
) : GestureDetector.SimpleOnGestureListener() {

    private val detector = GestureDetectorCompat(hostActivity, this)
    private val taps = ArrayDeque<Long>()
    private val density = hostActivity.resources.displayMetrics.density

    fun attachTo(view: View) {
        // aktifkan long-press threshold custom
        view.setOnTouchListener { _, ev ->
            if (enableLongPress && ev.action == MotionEvent.ACTION_DOWN) {
                view.postDelayed({
                    if (pressedInsideCorner(ev) && isStillDown(view)) requestPin()
                }, longPressMs)
            }
            detector.onTouchEvent(ev)
        }
    }

    private fun isStillDown(v: View) = v.isPressed || v.isPressed.not() // fallback; GestureDetector akan menangani up
    private fun pressedInsideCorner(ev: MotionEvent): Boolean {
        val limitPx = (cornerOnlyDp ?: 48) * density
        return cornerOnlyDp == null || (ev.x <= limitPx && ev.y <= limitPx)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (!enableTripleTap || !pressedInsideCorner(e)) return false
        val now = SystemClock.uptimeMillis()
        taps.addLast(now)
        while (taps.isNotEmpty() && now - taps.first() > windowMs) taps.removeFirst()
        if (taps.size >= tapsRequired) {
            taps.clear()
            requestPin()
            return true
        }
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        if (enableLongPress && pressedInsideCorner(e)) requestPin()
    }

    private fun requestPin() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(hostActivity)
        val savedPin = prefs.getString("adminPin", "2580")

        val input = EditText(hostActivity).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setEms(4)
        }

        AlertDialog.Builder(hostActivity)
            .setTitle("Admin")
            .setMessage("Masukkan PIN")
            .setView(input)
            .setPositiveButton("OK") { d, _ ->
                if (input.text.toString() == Prefs.pin(hostActivity)) onUnlocked()
                else Toast.makeText(hostActivity, "PIN salah", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
