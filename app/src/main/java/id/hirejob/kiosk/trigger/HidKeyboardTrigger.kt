package id.hirejob.kiosk.trigger

import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.annotation.MainThread

class HidKeyboardTrigger(
    private val dwellMs: Long = 200L
) {

    interface Callback { fun onTriggerChanged(active: Boolean) }

    private var callback: Callback? = null
    private var targetKeyCodes: Set<Int> = emptySet()
    private var lastDownTs: Long = 0L
    private var armed = true

    fun configure(usbHidKey: String?) {
        targetKeyCodes = parseUsbHidKey(usbHidKey)
    }

    fun setCallback(cb: Callback?) { callback = cb }

    /** Panggil dari Activity.dispatchKeyEvent saat source = USB_HID */
    @MainThread
    fun handleKeyEvent(ev: KeyEvent): Boolean {
        if (ev.action != KeyEvent.ACTION_DOWN || ev.repeatCount != 0) return false
        val code = ev.keyCode
        if (code !in targetKeyCodes) return false

        val now = SystemClock.uptimeMillis()
        if (!armed && kotlin.math.abs(now - lastDownTs) < dwellMs) return true

        lastDownTs = now
        armed = false
        callback?.onTriggerChanged(true)
        return true
    }

    fun reset() {
        armed = true
        callback?.onTriggerChanged(false)
    }


    private fun parseUsbHidKey(inputRaw: String?): Set<Int> {
        val input = (inputRaw ?: "F9").trim()
        if (input.isEmpty()) return setOf(KeyEvent.KEYCODE_F9)

        val upper = input.uppercase()

        // F1..F12
        if (upper.matches(Regex("^F(1[0-2]|[1-9])$"))) {
            val n = upper.removePrefix("F").toInt()
            return setOf(fKeyCode(n))
        }

        // single char
        if (input.length == 1) {
            val c = input.first()
            // A..Z
            val u = c.uppercaseChar()
            if (u in 'A'..'Z') return setOf(KeyEvent.KEYCODE_A + (u - 'A'))
            // 0..9
            if (u in '0'..'9') return setOf(KeyEvent.KEYCODE_0 + (u - '0'))
            // fallback pakai KeyCharacterMap
            val kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
            val events = kcm.getEvents(charArrayOf(c))
            val code = events?.firstOrNull()?.keyCode ?: KeyEvent.KEYCODE_F9
            return setOf(code)
        }

        // fallback: F9
        return setOf(KeyEvent.KEYCODE_F9)
    }

    private fun fKeyCode(n: Int): Int = when (n) {
        1 -> KeyEvent.KEYCODE_F1
        2 -> KeyEvent.KEYCODE_F2
        3 -> KeyEvent.KEYCODE_F3
        4 -> KeyEvent.KEYCODE_F4
        5 -> KeyEvent.KEYCODE_F5
        6 -> KeyEvent.KEYCODE_F6
        7 -> KeyEvent.KEYCODE_F7
        8 -> KeyEvent.KEYCODE_F8
        9 -> KeyEvent.KEYCODE_F9
        10 -> KeyEvent.KEYCODE_F10
        11 -> KeyEvent.KEYCODE_F11
        12 -> KeyEvent.KEYCODE_F12
        else -> KeyEvent.KEYCODE_F9
    }
}
