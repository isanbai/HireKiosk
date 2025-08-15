package id.hirejob.kiosk.core

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class UiState {
    data object IDLE : UiState()

    data object ACTIVE : UiState()
}

class StateMachine(
    private val scope: CoroutineScope,
    triggerFlow: Flow<Boolean>,
    private val debounceMs: Long,
    private val minActiveMs: Long,
    private val minIdleMs: Long,
) {
    private val _state = MutableStateFlow<UiState>(UiState.IDLE)
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        scope.launch {
            triggerFlow
                .debounce(debounceMs)
                .distinctUntilChanged()
                .collect { on ->
                    when (on) {
                        true -> goActive()
                        false -> goIdle()
                    }
                }
        }
    }

    private var lastActiveAt = 0L
    private var lastIdleAt = 0L

    private fun goActive() {
        scope.launch {
            val now = SystemClock.now()
            val sinceIdle = now - lastIdleAt
            if (sinceIdle < minIdleMs) delay(minIdleMs - sinceIdle)
            _state.value = UiState.ACTIVE
            lastActiveAt = SystemClock.now()
        }
    }

    private fun goIdle() {
        scope.launch {
            val now = SystemClock.now()
            val sinceActive = now - lastActiveAt
            if (sinceActive < minActiveMs) delay(minActiveMs - sinceActive)
            _state.value = UiState.IDLE
            lastIdleAt = SystemClock.now()
        }
    }

    private object SystemClock {
        fun now() = System.currentTimeMillis()
    }
}
