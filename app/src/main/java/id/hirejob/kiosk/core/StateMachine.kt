package id.hirejob.kiosk.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class UiState { data object IDLE: UiState(); data object ACTIVE: UiState() }

class StateMachine(
    private val scope: CoroutineScope,
    triggerFlow: Flow<Boolean>,
    private val debounceMs: Long,
    private val minActiveMs: Long,
    private val minIdleMs: Long
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
                        true  -> goActive()
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

    private object SystemClock { fun now() = System.currentTimeMillis() }
}
