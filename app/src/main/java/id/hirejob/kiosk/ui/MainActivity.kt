package id.hirejob.kiosk.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import id.hirejob.kiosk.core.*
import id.hirejob.kiosk.databinding.ActivityMainBinding
import id.hirejob.kiosk.image.ImageController
import id.hirejob.kiosk.kiosk.KioskHelper
import id.hirejob.kiosk.player.VideoController
import id.hirejob.kiosk.trigger.HttpTrigger
import id.hirejob.kiosk.trigger.VolumeTrigger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var video: VideoController? = null
    private var image: ImageController? = null
    private var volumeTrigger: VolumeTrigger? = null
    private var httpTrigger: HttpTrigger? = null
    private var sm: StateMachine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        immersive()

        video = VideoController(this, b.playerView)
        image = ImageController(b.imageView)
        volumeTrigger = VolumeTrigger(this)

        lifecycleScope.launch {
            val s = Prefs.readAll(this@MainActivity)
            // show current idle image at start
            image!!.show(s.imageUri?.let(Uri::parse))

            // choose trigger for UI process:
            val triggerFlow: Flow<Boolean> = when (s.trigger) {
                TriggerType.HTTP -> {
                    httpTrigger = HttpTrigger(s.httpPortStr).also { it.start() }
                    httpTrigger!!.isOn
                }
                TriggerType.VOLUME, TriggerType.USB_HID, TriggerType.BT_HID -> volumeTrigger!!.isOn
                else -> MutableStateFlow(false) // headset/ble to be added next steps
            }

            sm = StateMachine(
                scope = lifecycleScope,
                triggerFlow = triggerFlow,
                debounceMs = s.debounceMs.toLong(),
                minActiveMs = s.minActiveMs.toLong(),
                minIdleMs = s.minIdleMs.toLong()
            )

            sm!!.state.onEach { st ->
                when (st) {
                    UiState.ACTIVE -> {
                        b.playerView.visibility = View.VISIBLE
                        b.imageView.visibility = View.GONE
                        s.videoUri?.let { video!!.play(Uri.parse(it), s.loopVideo) }
                    }
                    UiState.IDLE -> {
                        video!!.stop()
                        b.playerView.visibility = View.GONE
                        b.imageView.visibility = View.VISIBLE
                        image!!.show(s.imageUri?.let(Uri::parse))
                    }
                }
                if (s.diagnostic) {
                    b.root.announceForAccessibility("State ${st::class.simpleName}")
                }
            }.launchIn(lifecycleScope)

            // start background service keep-alive if HTTP selected
            if (s.trigger == TriggerType.HTTP) {
                startForegroundService(Intent(this@MainActivity, KioskService::class.java))
            }

            // optional kiosk
            if (s.kiosk) KioskHelper.tryStartLockTask(this@MainActivity)
        }

        // tap to open settings
        b.root.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (volumeTrigger?.onKey(keyCode, event) == true) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        httpTrigger?.stop()
        video?.release()
        super.onDestroy()
    }

    private fun immersive() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}
