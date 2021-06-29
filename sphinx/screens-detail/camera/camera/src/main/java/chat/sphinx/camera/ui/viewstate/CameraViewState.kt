package chat.sphinx.camera.ui.viewstate

import chat.sphinx.camera.model.CameraItem
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class CameraViewState: ViewState<CameraViewState>() {
    object Idle: CameraViewState()

    sealed class Active: CameraViewState() {

        abstract val cameraItem: CameraItem?

        data class BackCamera(
            override val cameraItem: CameraItem?
        ): Active()

        data class FrontCamera(
            override val cameraItem: CameraItem?
        ): Active()
    }

}
