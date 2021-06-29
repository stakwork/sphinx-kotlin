package chat.sphinx.camera.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

sealed class ImagePreviewViewState: ViewState<ImagePreviewViewState>() {
    object None: ImagePreviewViewState()
    data class ImagePreview(val image: File): ImagePreviewViewState()
}
