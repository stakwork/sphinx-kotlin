package chat.sphinx.camera.ui

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera.coordinator.CameraViewModelCoordinator
import chat.sphinx.camera.model.CameraItem
import chat.sphinx.camera.model.LensFacing
import chat.sphinx.camera.ui.viewstate.CameraViewState
import chat.sphinx.camera.ui.viewstate.CapturePreviewViewState
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_view_model_coordinator.ResponseHolder
import chat.sphinx.feature_view_model_coordinator.RequestCatcher
import chat.sphinx.kotlin_response.Response
import chat.sphinx.logger.SphinxLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

internal suspend inline fun CameraViewModel.collectImagePreviewViewState(
    crossinline action: suspend (value: CapturePreviewViewState) -> Unit
): Unit =
    cameraPreviewViewStateContainer.collect { action(it) }

internal inline val CameraViewModel.currentCapturePreviewViewState: CapturePreviewViewState
    get() = cameraPreviewViewStateContainer.value

@Suppress("NOTHING_TO_INLINE")
internal inline fun CameraViewModel.updateImagePreviewViewState(viewState: CapturePreviewViewState) =
    cameraPreviewViewStateContainer.updateViewState(viewState)

@HiltViewModel
internal class CameraViewModel @Inject constructor(
    private val app: Application,
    private val applicationScope: CoroutineScope,
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

    private var responseJob: Job? = null
    fun processResponse(viewState: CapturePreviewViewState.Preview) {
        if (responseJob?.isActive == true) {
            return
        }

        responseJob = viewModelScope.launch(mainImmediate) {

            try {
                requestCatcher.getCaughtRequestStateFlow().collect { list ->
                    list.firstOrNull()?.let { requestHolder ->
                        cameraCoordinator.submitResponse(
                            response = Response.Success(
                                ResponseHolder(
                                    requestHolder,
                                    CameraResponse(viewState.value)
                                )
                            ),
                            navigateBack = Any(),
                        )
                    }
                }
            } catch (e: Exception) {}
        }
    }

    val cameraManager: CameraManager by lazy {
        app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun enumerateCameras(): List<CameraItem> {
        val ids = cameraManager.cameraIdList.filter {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
        }

        val cameras: MutableList<CameraItem> = ArrayList(ids.size)

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
                    CameraItem(
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

    fun getFrontCamera(): CameraItem? {
        return cameras.lastOrNull { it.lensFacing == LensFacing.Front }
    }

    fun getBackCamera(): CameraItem? {
        return cameras.lastOrNull { it.lensFacing == LensFacing.Back }
    }

    val cameraPreviewViewStateContainer: ViewStateContainer<CapturePreviewViewState> by lazy {
        ViewStateContainer(CapturePreviewViewState.None)
    }

    fun deleteImage(image: File) {
        viewModelScope.launch(io) {
            if (image.isFile) {
                image.delete()
            }
        }
    }

    private val cameraDir = File(app.cacheDir, "camera_cache").also {
        it.mkdirs()
    }

    fun createFile(extension: String, image: Boolean): File {
        val sdf = SimpleDateFormat("yyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        if (!cameraDir.exists()) {
            cameraDir.mkdirs()
        }
        return File(cameraDir, "${if (image) "IMG" else "VID"}_${sdf.format(Date())}.$extension")
    }

    override fun onCleared() {
        super.onCleared()
        if (responseJob == null) {
            @Exhaustive
            when (val vs = currentCapturePreviewViewState) {
                is CapturePreviewViewState.None -> {}
                is CapturePreviewViewState.Preview -> {
                    // user hit back button, so no file was returned
                    applicationScope.launch(dispatchers.io) {
                        try {
                            vs.value.delete()
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }
}
