package chat.sphinx.chat_common.ui.viewstate.attachment

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import chat.sphinx.wrapper_message_media.FileName
import chat.sphinx.wrapper_message_media.MessageMedia
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class
AttachmentFullscreenViewState: ViewState<AttachmentFullscreenViewState>() {

    object Idle: AttachmentFullscreenViewState()

    data class ImageFullscreen(
        val url: String,
        val media: MessageMedia?
    ): AttachmentFullscreenViewState()

    data class PdfFullScreen(
        val fileName: FileName,
        val pageCount: Int,
        val currentPage: Int,
        val pdfRender: PdfRenderer
    ): AttachmentFullscreenViewState()

}
