package chat.sphinx.menu_bottom_profile_pic

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.wrapper_io_utils.InputStreamProvider
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.toMediaType
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

interface PictureMenuViewModel {
    val pictureMenuHandler: PictureMenuHandler
}

class PictureMenuHandler(
    private val app: Application,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    dispatchers: CoroutineDispatchers,
    private val viewModel: ViewModel,

    private val callback: (
        streamProvider: InputStreamProvider,
        mediaType: MediaType,
        fileName: String,
        contentLength: Long?,

        // A File is returned only if coming from the camera
        file: File?
    ) -> Unit

) : CoroutineDispatchers by dispatchers {

    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }

    private var cameraJob: Job? = null
    fun updatePictureFromCamera() {
        if (cameraJob?.isActive == true) {
            return
        }

        cameraJob = viewModel.viewModelScope.launch(mainImmediate) {
            val response = cameraCoordinator.submitRequest(CameraRequest)

            @Exhaustive
            when (response) {
                is Response.Error -> {}
                is Response.Success -> {

                    @Exhaustive
                    when (response.value) {
                        is CameraResponse.Image -> {

                            val ext = response.value.value.extension
                            val mediaType = MediaType.Image(MediaType.IMAGE + "/$ext")

                            viewStateContainer.updateViewState(MenuBottomViewState.Closed)

                            callback.invoke(
                                object : InputStreamProvider() {
                                    override fun newInputStream(): InputStream {
                                        return response.value.value.inputStream()
                                    }
                                },
                                mediaType,
                                response.value.value.name,
                                response.value.value.length(),
                                response.value.value,
                            )
                        }
                        is CameraResponse.Video -> {
                            val ext = response.value.value.extension
                            val mediaType = MediaType.Image(MediaType.VIDEO + "/$ext")

                            viewStateContainer.updateViewState(MenuBottomViewState.Closed)

                            callback.invoke(
                                object : InputStreamProvider() {
                                    override fun newInputStream(): InputStream {
                                        return response.value.value.inputStream()
                                    }
                                },
                                mediaType,
                                response.value.value.name,
                                response.value.value.length(),
                                response.value.value,
                            )
                        }
                    }

                }
            }
        }
    }

    fun updatePictureFromPhotoLibrary(uri: Uri?) {
        if (uri == null) {
            return
        }

        val cr = app.contentResolver

        cr.getType(uri)?.let { crType ->

            MimeTypeMap.getSingleton().getExtensionFromMimeType(crType)?.let { ext ->

                crType.toMediaType().let { mType ->

                    @Exhaustive
                    when (mType) {
                        is MediaType.Image -> {
                            val stream: InputStream? = try {
                                cr.openInputStream(uri)
                            } catch (e: Exception) {
                                // TODO: Handle Error
                                null
                            }

                            if (stream != null) {
                                viewStateContainer.updateViewState(MenuBottomViewState.Closed)


                                callback.invoke(
                                    object : InputStreamProvider() {
                                        private var initialStreamUsed: Boolean = false
                                        override fun newInputStream(): InputStream {
                                            return if (!initialStreamUsed) {
                                                initialStreamUsed = true
                                                stream
                                            } else {
                                                cr.openInputStream(uri)!!
                                            }
                                        }
                                    },
                                    mType,
                                    "image.$ext",
                                    null,
                                    null
                                )
                            }
                        }
                        is MediaType.Audio,
                        is MediaType.Pdf,
                        is MediaType.Text,
                        is MediaType.Unknown,
                        is MediaType.Video -> {
                            // do nothing
                        }
                    }
                }

            }
        }
    }
}

