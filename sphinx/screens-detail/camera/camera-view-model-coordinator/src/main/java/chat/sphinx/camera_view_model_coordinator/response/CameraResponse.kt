package chat.sphinx.camera_view_model_coordinator.response

import java.io.File

sealed class CameraResponse {

    abstract val value: File

    data class Image(override val value: File): CameraResponse()
    data class Video(override val value: File): CameraResponse()
}