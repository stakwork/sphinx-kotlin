package chat.sphinx.camera.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

sealed class CapturePreviewViewState: ViewState<CapturePreviewViewState>() {

    object None: CapturePreviewViewState()

    sealed class Preview: CapturePreviewViewState() {

        abstract val media: File

        data class ImagePreview(override val media: File): Preview()

        data class VideoPreview(override val media: File): Preview()
    }

}
