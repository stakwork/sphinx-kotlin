package chat.sphinx.camera.ui

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera.coordinator.CameraViewModelCoordinator
import chat.sphinx.camera.ui.viewstate.CameraViewState
import chat.sphinx.feature_view_model_coordinator.RequestCatcher
import chat.sphinx.logger.SphinxLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class CameraViewModel @Inject constructor(
    private val app: Application,
    dispatchers: CoroutineDispatchers,
    private val cameraCoordinator: CameraViewModelCoordinator,
    private val LOG: SphinxLogger,
): SideEffectViewModel<
        FragmentActivity,
        CameraSideEffect,
        CameraViewState,
        >(dispatchers, CameraViewState.Idle)
{

    companion object {
        const val TAG = "CameraViewModel"
    }

    private val requestCatcher = RequestCatcher(
        viewModelScope,
        cameraCoordinator,
        mainImmediate,
    )

    val cameraManager: CameraManager by lazy {
        app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    sealed class LensFacing {
        object Front: LensFacing()
        object Back: LensFacing()
    }

    data class CameraListItem(
        val cameraId: String,
        val lensFacing: LensFacing,
        val characteristics: CameraCharacteristics,
        val capabilities: IntArray,
        val configMap: StreamConfigurationMap,
        val outputFormats: IntArray,
        // TODO: List video output data
    )

    private fun enumerateCameras(): List<CameraListItem> {
        val ids = cameraManager.cameraIdList.filter {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
        }

        val cameras: MutableList<CameraListItem> = ArrayList(ids.size)

        ids.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val orientation: LensFacing? = characteristics.get(CameraCharacteristics.LENS_FACING).let {
                when (it) {
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        LensFacing.Back
                    }
                    CameraCharacteristics.LENS_FACING_FRONT -> {
                        LensFacing.Front
                    }
                    else -> {
                        null
                    }
                }
            }

            orientation?.let { nnOrientation ->
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!

                // TODO: get video support capabilities and add as a parameter to CameraListItem

                cameras.add(
                    CameraListItem(
                        id,
                        nnOrientation,
                        characteristics,
                        capabilities,
                        configMap,
                        configMap.outputFormats
                    )
                )
            }
        }

        return cameras
    }

    private val cameras = enumerateCameras()

    fun getFrontCamera(): CameraListItem? {
        return cameras.lastOrNull { it.lensFacing == LensFacing.Front }
    }

    fun getBackCamera(): CameraListItem? {
        return cameras.lastOrNull { it.lensFacing == LensFacing.Back }
    }
}
