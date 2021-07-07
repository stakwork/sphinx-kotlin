package chat.sphinx.chat_common.ui.viewstate.attachment

import chat.sphinx.wrapper_message_media.MediaType
import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

internal sealed class AttachmentSendViewState: ViewState<AttachmentSendViewState>() {

    object Idle: AttachmentSendViewState()

    data class Preview(val file: File, val type: MediaType): AttachmentSendViewState()
}
