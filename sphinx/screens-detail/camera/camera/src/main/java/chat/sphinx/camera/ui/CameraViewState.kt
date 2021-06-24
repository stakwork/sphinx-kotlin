package chat.sphinx.camera.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class CameraViewState: ViewState<CameraViewState>() {
    object Idle: CameraViewState()
}
