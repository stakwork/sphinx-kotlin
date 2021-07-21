package chat.sphinx.menu_bottom_tribe_profile_pic

import android.app.Application
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.toMediaType
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
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
    private val mediaCacheHandler: MediaCacheHandler,
    private val viewModelScope: CoroutineScope,
) {
    companion object {
        const val TAG = "TribeProfilePicMenuHandler"
    }
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }

    private var cameraJob: Job? = null

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
                            val mediaType = MediaType.Image(
                                "${MediaType.IMAGE}/${response.value.value.extension}"
                            )

                            chatSharedFlow.collect { chat ->
                                try {
                                    val repoResponse = chatRepository.updateChatProfilePic(
                                        chat!!,
                                        file = response.value.value,
                                        mediaType = mediaType
                                    )

                                    @Exhaustive
                                    when (repoResponse) {
                                        is Response.Error -> {
                                            // TODO: Handle Error
                                            Log.e(TAG, "Error update chat Profile Picture: ", repoResponse.cause.exception)
                                        }
                                        is Response.Success -> {
                                            // TODO: Tell user it was successful
                                            //  update the chat
                                        }
                                    }
                                } catch (e: Exception) {
                                    // TODO: Handle error
                                    Log.e(TAG, "Error camera picture: ", e)
                                }
                                try {
                                    // Make sure we detail the new image
                                    response.value.value.delete()
                                } catch (e: Exception) {}
                            }
                            viewStateContainer.updateViewState(MenuBottomViewState.Closed)
                        }
                    }
                }
            }
        }
    }

    fun handleActivityResultUri(uri: Uri?) {
        if (uri == null) {
            return
        }

        val cr = app.contentResolver

        cr.getType(uri)?.let { crType ->

            MimeTypeMap.getSingleton().getExtensionFromMimeType(crType)?.let { ext ->

                val stream: InputStream = try {
                    cr.openInputStream(uri) ?: return
                } catch (e: Exception) {
                    return
                }

                crType.toMediaType().let { mType ->
                    @Exhaustive
                    when (mType) {
                        is MediaType.Image -> {
                            viewModelScope.launch(dispatchers.mainImmediate) {
                                chatSharedFlow.collect { chat ->
                                    val newFile: File = mediaCacheHandler.createImageFile(ext)

                                    try {
                                        mediaCacheHandler.copyTo(stream, newFile)
                                        val repoResponse = chatRepository.updateChatProfilePic(
                                            chat!!,
                                            file = newFile,
                                            mediaType = mType
                                        )

                                        @Exhaustive
                                        when (repoResponse) {
                                            is Response.Error -> {
                                                // TODO: Handle Error
                                                Log.e(TAG, "Error update chat Profile Picture: ", repoResponse.cause.exception)
                                            }
                                            is Response.Success -> {
                                                // TODO: Tell user it was successful
                                                //  update the chat
                                            }
                                        }
                                    } catch (e: Exception) {
                                        newFile.delete()
                                        Log.e(
                                            TAG,
                                            "Failed to copy content to new file: ${newFile.path}",
                                            e
                                        )
                                    }
                                }
                            }
                            viewStateContainer.updateViewState(MenuBottomViewState.Closed)
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
