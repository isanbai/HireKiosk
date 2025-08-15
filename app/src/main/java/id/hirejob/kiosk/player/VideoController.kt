package id.hirejob.kiosk.player

import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

class VideoController(
    ctx: Context,
    private val playerView: PlayerView,
) {
    private val exo =
        ExoPlayer.Builder(ctx).build().apply {
            volume = 0f // muted by default
            playerView.player = this
            playerView.useController = false
            // ⬇️ Tambahan agar video full-bleed
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

            playerView.setShutterBackgroundColor(Color.TRANSPARENT)
            playerView.setKeepContentOnPlayerReset(true)
        }

    private var endListener: Player.Listener? = null

    /**
     * Play the given [uri].
     * If [loop] is true, repeat forever. Otherwise, play once then invoke [onEnded].
     */
    fun play(
        uri: Uri,
        loop: Boolean,
        onEnded: (() -> Unit)? = null,
    ) {
        // Bersihkan listener lama agar tidak dobel
        endListener?.let { exo.removeListener(it) }
        endListener = null

        exo.setMediaItem(MediaItem.fromUri(uri))
        exo.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        exo.prepare()
        exo.playWhenReady = true
        // exo.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

        if (!loop && onEnded != null) {
            endListener =
                object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            // Lepas listener supaya hanya sekali
                            endListener?.let { exo.removeListener(it) }
                            endListener = null
                            onEnded.invoke()
                        }
                    }
                }
            exo.addListener(endListener!!)
        }
    }

    fun stop() {
        // Lepas listener supaya aman
        endListener?.let { exo.removeListener(it) }
        endListener = null

        exo.playWhenReady = false
        exo.stop()
        exo.clearMediaItems()
    }

    fun release() {
        endListener?.let { exo.removeListener(it) }
        endListener = null

        playerView.player = null
        exo.release()
    }
}
