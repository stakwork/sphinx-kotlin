package chat.sphinx.camera.ui.viewstate

import chat.sphinx.camera.ui.CameraViewModel
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class CameraViewState: ViewState<CameraViewState>() {
    object Idle: CameraViewState()

    sealed class Active: CameraViewState() {

        abstract val cameraListItem: CameraViewModel.CameraListItem?

        data class BackCamera(
            override val cameraListItem: CameraViewModel.CameraListItem?
        ): Active()

        data class FrontCamera(
            override val cameraListItem: CameraViewModel.CameraListItem?
        ): Active()
    }

}
