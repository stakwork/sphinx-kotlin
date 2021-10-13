package chat.sphinx.chat_common.util

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import java.io.File

class VideoThumbnailUtil {
    companion object {
        fun loadThumbnail(file: File): Bitmap? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.createVideoThumbnail(
                    file,
                    Size(1000, 1000),
                    null
                )
            } else {
                ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Video.Thumbnails.MINI_KIND)
            }

        }
    }
}