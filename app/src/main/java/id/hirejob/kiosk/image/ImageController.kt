package id.hirejob.kiosk.image

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide

class ImageController(private val imageView: ImageView) {
    fun show(uri: Uri?) {
        if (uri == null) {
            imageView.setImageDrawable(null)
            return
        }
        Glide.with(imageView).asDrawable().load(uri).into(imageView)
    }
}
