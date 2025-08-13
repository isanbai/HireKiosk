package id.hirejob.kiosk.trigger

import android.app.Activity
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VolumeTrigger(private val activity: Activity) : TriggerSource {
    private val _isOn = MutableStateFlow(false)
    override val isOn = _isOn.asStateFlow()
    override fun start() { /* handled via activity dispatch */ }
    override fun stop() { /* no-op */ }

    // Tambahan: agar MainActivity bisa mematikan ketika video (non-loop) selesai
    fun setState(on: Boolean) { _isOn.value = on }

    fun onKey(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> { 
                    _isOn.value = true; 
                    return true 
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> { 
                    // _isOn.value = false; 
                    _isOn.value = !_isOn.value
                    return true 
                }
            }
        }
        return false
    }
}
