package chat.sphinx.camera.model

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap

sealed class LensFacing {
    object Back: LensFacing()
    object Front: LensFacing()

    fun toInt(): Int {
        return when(this) {
            is Back -> {
                CameraCharacteristics.LENS_FACING_BACK
            }
            is Front -> {
                CameraCharacteristics.LENS_FACING_FRONT
            }
        }
    }
}

data class CameraItem(
    val cameraId: String,
    val lensFacing: LensFacing,
    val characteristics: CameraCharacteristics,
    val capabilities: IntArray,
    val configMap: StreamConfigurationMap,
    val outputFormats: IntArray,
    // TODO: List video output data
)
