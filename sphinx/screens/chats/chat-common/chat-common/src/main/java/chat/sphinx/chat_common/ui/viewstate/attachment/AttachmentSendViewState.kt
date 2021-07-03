package chat.sphinx.chat_common.ui.viewstate.attachment

import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class AttachmentSendViewState: ViewState<AttachmentSendViewState>() {

    object Idle: AttachmentSendViewState()

    sealed class Preview: AttachmentSendViewState() {
        data class LocalFile(val cameraResponse: CameraResponse): Preview()
    }
}
