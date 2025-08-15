package id.hirejob.kiosk.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * POWER trigger:
 *  - Default: ON saat CHARGING/FULL
 *  - invert=true: ON saat DISCHARGING (kebalikan)
 *
 * Hanya mendengar ACTION_BATTERY_CHANGED (sticky + kontinu).
 * Menentukan "charging" prioritaskan EXTRA_PLUGGED (AC/USB/WIRELESS != 0),
 * lalu fallback ke EXTRA_STATUS (CHARGING/FULL).
 */
class PowerTrigger(
    private val ctx: Context,
    private val invert: Boolean = false,
) : TriggerSource {
    companion object {
        private const val TAG = "PowerTrigger"
    }

    private val _isOn = MutableStateFlow(false)
    override val isOn = _isOn.asStateFlow()

    private var registered = false

    private val rx =
        object : BroadcastReceiver() {
            override fun onReceive(
                c: Context?,
                i: Intent?,
            ) {
                val raw = isRawCharging(i)
                val eff = if (invert) !raw else raw
                Log.d(TAG, "onReceive(): action=${i?.action} status=${statusString(i)} raw=$raw invert=$invert -> isOn=$eff")
                _isOn.value = eff
            }
        }

    override fun start() {
        if (registered) return

        // Ambil state awal dari sticky intent
        val sticky = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val raw = isRawCharging(sticky)
        val eff = if (invert) !raw else raw
        Log.d(TAG, "start(): sticky status=${statusString(sticky)} raw=$raw invert=$invert -> isOn=$eff")
        _isOn.value = eff

        // Dengarkan perubahan baterai (saja)
        ctx.registerReceiver(rx, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registered = true
        Log.d(TAG, "start(): receiver registered for BATTERY_CHANGED only")
    }

    override fun stop() {
        if (!registered) return
        try {
            ctx.unregisterReceiver(rx)
        } catch (_: Throwable) {
        }
        registered = false
    }

    /** True jika perangkat tercolok daya (AC/USB/WIRELESS) atau status CHARGING/FULL. */
    private fun isRawCharging(i: Intent?): Boolean {
        if (i == null) return false
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        // 0 = unplugged, 1 = AC, 2 = USB, 4 = wireless (bitmask)
        return plugged != 0
    }

    /** Teks bantu untuk log: status + plugged. */
    private fun statusString(i: Intent?): String {
        if (i == null) return "null"
        val s = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val p = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val sTxt =
            when (s) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING(2)"
                BatteryManager.BATTERY_STATUS_FULL -> "FULL(5)"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING(3)"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING(4)"
                BatteryManager.BATTERY_STATUS_UNKNOWN -> "UNKNOWN(1)"
                else -> "($s)"
            }
        // plugged: 0=unplugged, 1=AC, 2=USB, 4=wireless (bitmask)
        return "$sTxt plugged=$p"
    }
}
