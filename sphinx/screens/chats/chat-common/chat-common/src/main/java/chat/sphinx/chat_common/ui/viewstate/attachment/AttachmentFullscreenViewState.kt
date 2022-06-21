package chat.sphinx.chat_common.ui.viewstate.attachment

import chat.sphinx.wrapper_message_media.MessageMedia
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class
AttachmentFullscreenViewState: ViewState<AttachmentFullscreenViewState>() {

    object Idle: AttachmentFullscreenViewState()

    data class Fullscreen(
        val url: String,
        val media: MessageMedia?
    ): AttachmentFullscreenViewState()

}
