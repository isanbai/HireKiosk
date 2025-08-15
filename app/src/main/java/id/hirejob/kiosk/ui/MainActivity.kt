package id.hirejob.kiosk.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import id.hirejob.kiosk.R
import id.hirejob.kiosk.core.KioskWatcher
import id.hirejob.kiosk.core.Prefs
import id.hirejob.kiosk.core.SecretGate
import id.hirejob.kiosk.core.StateMachine
import id.hirejob.kiosk.core.TriggerType
import id.hirejob.kiosk.core.UiState
import id.hirejob.kiosk.databinding.ActivityMainBinding
import id.hirejob.kiosk.device.KioskPolicy
import id.hirejob.kiosk.image.ImageController
import id.hirejob.kiosk.kiosk.KioskHelper
import id.hirejob.kiosk.player.VideoController
import id.hirejob.kiosk.settings.SettingsActivity
import id.hirejob.kiosk.trigger.HidKeyboardTrigger
import id.hirejob.kiosk.trigger.HttpTrigger
import id.hirejob.kiosk.trigger.PowerTrigger
import id.hirejob.kiosk.trigger.VolumeTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var cachedTrigger: TriggerType = TriggerType.VOLUME
    private var cachedUsbHidKey: String = "F9"

    private lateinit var b: ActivityMainBinding
    private var overlayView: View? = null
    private var hasAutoPlayed = false
    private var video: VideoController? = null
    private var image: ImageController? = null
    private var volumeTrigger: VolumeTrigger? = null
    private var powerTrigger: PowerTrigger? = null
    private var hidTrigger: HidKeyboardTrigger? = null
    private val hidFlow = MutableStateFlow(false)
    private var httpTrigger: HttpTrigger? = null
    private var sm: StateMachine? = null
    private lateinit var gate: SecretGate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // startForegroundService(Intent(this, KioskService::class.java))
        startForegroundService(Intent(this, KioskWatcher::class.java))
        enableImmersive()
        supportActionBar?.hide()
        // Log.d("MainActivity", "onCreate() — starting UI")

        val root = findViewById<View>(android.R.id.content) // seluruh layar
        gate =
            SecretGate(
                hostActivity = this,
                onUnlocked = { startActivity(Intent(this, SettingsActivity::class.java)) },
                enableTripleTap = true,
                enableLongPress = true,
                cornerOnlyDp = 48,
            )
        gate.attachTo(root)

        KioskPolicy.apply(this)
        applyKioskUi()
        // hideSystemBars()
        // startKioskModeIfPossible()
        // ensureKioskService()

        video = VideoController(this, b.playerView)
        image = ImageController(b.imageView)
        overlayView = layoutInflater.inflate(R.layout.overlay_logo, null)

        volumeTrigger = VolumeTrigger(this)
        powerTrigger = PowerTrigger(this)

        lifecycleScope.launch {
            val s = Prefs.readAll(this@MainActivity)
            cachedTrigger = s.trigger
            cachedUsbHidKey = s.usbHidKey

            Log.d(
                "MainActivity",
                "settings: trigger=${s.trigger} powerInvert=${s.powerInvert} loop=${s.loopVideo} video=${s.videoUri} image=${s.imageUri}",
            )

            // show current idle image at start
            image!!.show(s.imageUri?.let(Uri::parse))

            val triggerFlow: Flow<Boolean> =
                when (s.trigger) {
                    TriggerType.HTTP -> {
                        httpTrigger = HttpTrigger(s.httpPortStr).also { it.start() }
                        httpTrigger!!.isOn
                    }
                    TriggerType.POWER -> {
                        Log.d("MainActivity", "Using POWER trigger (invert=${s.powerInvert})")
                        powerTrigger = PowerTrigger(applicationContext, invert = s.powerInvert).also { it.start() }
                        powerTrigger!!.isOn
                    }
                    TriggerType.USB_HID -> {
                        if (hidTrigger == null) {
                            hidTrigger =
                                HidKeyboardTrigger(dwellMs = s.debounceMs.toLong()).apply {
                                    configure(cachedUsbHidKey)
                                    setCallback(
                                        object : HidKeyboardTrigger.Callback {
                                            override fun onTriggerChanged(active: Boolean) {
                                                hidFlow.value = active // update flow
                                            }
                                        },
                                    )
                                }
                        } else {
                            hidTrigger?.configure(cachedUsbHidKey)
                        }
                        hidFlow // <-- ini yang return, sesuai tipe Flow<Boolean>
                    }

                    TriggerType.VOLUME, TriggerType.BT_HID -> volumeTrigger!!.isOn
                    else -> MutableStateFlow(false)
                }

            sm =
                StateMachine(
                    scope = lifecycleScope,
                    triggerFlow = triggerFlow,
                    debounceMs = s.debounceMs.toLong(),
                    minActiveMs = s.minActiveMs.toLong(),
                    minIdleMs = s.minIdleMs.toLong(),
                )

            sm!!.state.onEach { st ->
                Log.d("MainActivity", "SM state = $st")
                when (st) {
                    UiState.ACTIVE -> {
                        b.playerView.visibility = View.VISIBLE
                        b.imageView.visibility = View.GONE
                        (overlayView?.parent as? ViewGroup)?.removeView(overlayView)
                        s.videoUri?.let { uriStr ->
                            video!!.play(Uri.parse(uriStr), s.loopVideo) {
                                // play-once -> balik ke IDLE
                                if (!s.loopVideo) {
                                    when (s.trigger) {
                                        TriggerType.VOLUME -> volumeTrigger?.setState(false)
                                        // Edge-based: kamu boleh reset agar kembali ke IDLE
                                        TriggerType.USB_HID -> hidTrigger?.reset() // siapkan fungsi reset bila perlu
                                        TriggerType.HTTP -> httpTrigger?.stop() // atau _isOn.value=false jika kamu jadikan Flow
                                        // Level-based: biarkan mengikuti sumber
                                        TriggerType.POWER -> { /* no-op */ }
                                        else -> { /* HEADSET/BT/BLE bila nanti ada */ }
                                    }
                                }
                            }
                        }
                    }
                    UiState.IDLE -> {
                        video!!.stop()
                        b.playerView.visibility = View.GONE
                        b.imageView.visibility = View.VISIBLE
                        image!!.show(s.imageUri?.let(Uri::parse))
                        // tampilkan logo overlay
                        val logo = findViewById<ViewGroup>(android.R.id.content)
                        if (overlayView?.parent == null) {
                            logo.addView(overlayView)
                        }
                    }
                }
                if (s.diagnostic) b.root.announceForAccessibility("State ${st::class.simpleName}")
            }.launchIn(lifecycleScope)

            // === AUTO-PLAY SEKALI SETELAH SM SIAP ===
            if (s.trigger == TriggerType.VOLUME) {
                volumeTrigger?.setState(true)
            }

            // (Opsional) Start service kalau HTTP
            if (s.trigger == TriggerType.HTTP) {
                // startForegroundService(Intent(this@MainActivity, KioskService::class.java))
            }

            if (s.kiosk) KioskHelper.tryStartLockTask(this@MainActivity)

            // (opsional) dengarkan perubahan setting agar live-update tanpa restart activity
            launch {
                Prefs.triggerSource(this@MainActivity).collect { str ->
                    // sesuaikan parser kamu; contoh:
                    cachedTrigger = TriggerType.valueOf(str)
                }
            }
            launch {
                Prefs.usbHidKey(this@MainActivity).collect { key ->
                    cachedUsbHidKey = key
                    hidTrigger?.configure(key)
                }
            }
        }
    }

    // + intercept semua KeyEvent untuk USB_HID
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // biarkan volume ke handler lama
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // pass
        } else {
            if (cachedTrigger == TriggerType.USB_HID) {
                if (hidTrigger?.handleKeyEvent(event) == true) return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // Teruskan ke VolumeTrigger; jika di-handle, konsumsi event
            return volumeTrigger?.onKey(keyCode, event) ?: true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // Konsumsi up-event agar sistem tidak mengubah volume
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        volumeTrigger?.stop()
        video?.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val root = findViewById<View>(android.R.id.content)
        root.isFocusableInTouchMode = true
        root.requestFocus()
        enableImmersive()
        startKioskModeIfPossible()
        // ensureKioskService()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersive()
        hideSystemBars()
        // Re-apply immersive mode in case user swiped down the system bars
    }

    override fun onBackPressed() {
        // jangan apa-apa, biar ga bisa keluar
    }
}
