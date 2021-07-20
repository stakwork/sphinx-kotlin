package chat.sphinx.menu_bottom_tribe_profile_pic

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_io_utils.InputStreamProvider
import chat.sphinx.wrapper_io_utils.toInputStreamProvider
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.toMediaType
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.InputStream

interface TribeProfilePicMenuViewModel {
    val tribeProfilePicMenuHandler: TribeProfilePicMenuHandler
    val dispatchers: CoroutineDispatchers
}

class TribeProfilePicMenuHandler(
    private val app: Application,
    private val chatSharedFlow: SharedFlow<Chat?>,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    private val chatRepository: ChatRepository,
    private val dispatchers: CoroutineDispatchers,
    private val viewModelScope: CoroutineScope,
) {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }

    private var cameraJob: Job? = null

    @Suppress("BlockingMethodInNonBlockingContext")
    fun updateProfilePicCamera() {
        if (cameraJob?.isActive == true) {
            return
        }

        cameraJob = viewModelScope.launch(dispatchers.mainImmediate) {
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

                            val stream: FileInputStream? = try {
                                FileInputStream(response.value.value)
                            } catch (e: Exception) {
                                // TODO: Handle error
                                null
                            }

                            if (stream != null) {
                                viewStateContainer.updateViewState(MenuBottomViewState.Closed)

                                viewModelScope.launch(dispatchers.mainImmediate) {
                                    chatSharedFlow.collect {
                                        if (it != null) {
                                            val repoResponse = chatRepository.updateChatProfilePic(
                                                it,
                                                stream = stream.toInputStreamProvider(),
                                                mediaType = mediaType,
                                                fileName = response.value.value.name,
                                                contentLength = response.value.value.length(),
                                            )

                                            @Exhaustive
                                            when (repoResponse) {
                                                is Response.Error -> {
                                                    // TODO: Handle Error
                                                }
                                                is Response.Success -> {}
                                            }
                                        }
                                        try {
                                            response.value.value.delete()
                                        } catch (e: Exception) {}
                                    }
                                }.join()
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun handleActivityResultUri(uri: Uri?) {
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

                                viewModelScope.launch(dispatchers.mainImmediate) {
                                    chatSharedFlow.collect {
                                        if (it != null) {
                                            val repoResponse = chatRepository.updateChatProfilePic(
                                                it,
                                                stream = object : InputStreamProvider() {
                                                    var initialStreamUsed: Boolean = false
                                                    override fun newInputStream(): InputStream {
                                                        return if (!initialStreamUsed) {
                                                            initialStreamUsed = true
                                                            stream
                                                        } else {
                                                            cr.openInputStream(uri)!!
                                                        }
                                                    }
                                                },
                                                mediaType = mType,
                                                fileName = "image.$ext",
                                                contentLength = null,
                                            )

                                            @Exhaustive
                                            when (repoResponse) {
                                                is Response.Error -> {
                                                    // TODO: Handle Error...
                                                }
                                                is Response.Success -> {

                                                }
                                            }
                                        }
                                    }

                                }
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
