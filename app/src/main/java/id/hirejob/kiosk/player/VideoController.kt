package id.hirejob.kiosk.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class VideoController(
    ctx: Context,
    private val playerView: PlayerView
) {
    private val exo = ExoPlayer.Builder(ctx).build().apply {
        volume = 0f // muted by default
        playerView.player = this
        playerView.useController = false
    }

    fun play(uri: Uri, loop: Boolean) {
        val item = MediaItem.fromUri(uri)
        exo.setMediaItem(item)
        exo.repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ALL else ExoPlayer.REPEAT_MODE_OFF
        exo.prepare()
        exo.playWhenReady = true
    }

    fun stop() {
        exo.playWhenReady = false
        exo.stop()
        exo.clearMediaItems()
    }

    fun release() {
        playerView.player = null
        exo.release()
    }
}
