package chat.sphinx.send_attachment.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SendAttachmentViewState: ViewState<SendAttachmentViewState>() {
    object Idle: SendAttachmentViewState()
}