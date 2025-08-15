package id.hirejob.kiosk.trigger

import kotlinx.coroutines.flow.Flow

interface TriggerSource {
    fun start()

    fun stop()

    val isOn: Flow<Boolean>
}
