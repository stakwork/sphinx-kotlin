package chat.sphinx.tribe_detail.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom_tribe_profile_pic.TribeProfilePicMenuHandler
import chat.sphinx.menu_bottom_tribe_profile_pic.TribeProfilePicMenuViewModel
import chat.sphinx.tribe.TribeMenuHandler
import chat.sphinx.tribe.TribeMenuViewModel
import chat.sphinx.tribe_detail.R
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.toMediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

internal inline val TribeDetailFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class TribeDetailViewModel @Inject constructor(
    private val app: Application,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    private val contactRepository: ContactRepository,
    private val mediaCacheHandler: MediaCacheHandler,
    val navigator: TribeDetailNavigator,
): BaseViewModel<TribeDetailViewState>(dispatchers, TribeDetailViewState.Idle),
    TribeMenuViewModel,
    TribeProfilePicMenuViewModel
{
    companion object {
        const val TAG = "TribeDetailViewModel"
    }
    private var cameraJob: Job? = null
    private val args: TribeDetailFragmentArgs by savedStateHandle.navArgs()

    val chatId = args.chatId
    val podcast = args.argPodcast

    private val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {}
                delay(25L)

                resolvedOwner!!
            }
        }
    }

    private suspend fun getChat(): Chat {
        chatSharedFlow.replayCache.firstOrNull()?.let { chat ->
            return chat
        }

        chatSharedFlow.firstOrNull()?.let { chat ->
            return chat
        }

        var chat: Chat? = null

        try {
            chatSharedFlow.collect {
                if (it != null) {
                    chat = it
                    throw Exception()
                }
            }
        } catch (e: Exception) {}
        delay(25L)

        return chat!!
    }

    fun load() {
        viewModelScope.launch(mainImmediate) {
            updateViewState(TribeDetailViewState.Tribe(getChat(), getOwner(), podcast))
        }
    }

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_media_library)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    override val tribeMenuHandler: TribeMenuHandler by lazy {
        TribeMenuHandler(
            app,
            dispatchers,
            viewModelScope,
        )
    }

    override val tribeProfilePicMenuHandler: TribeProfilePicMenuHandler by lazy {
        TribeProfilePicMenuHandler()
    }

    override fun updateProfilePicCamera() {
        if (cameraJob?.isActive == true) {
            return
        }

        cameraJob = viewModelScope.launch(dispatchers.mainImmediate) {
            val response = cameraCoordinator.submitRequest(CameraRequest)

            updateViewState(
                TribeDetailViewState.TribeProfileUpdating
            )
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

                            try {
                                val repoResponse = chatRepository.updateChatProfilePic(
                                    getChat(),
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
                                        updateViewState(
                                            TribeDetailViewState.Tribe(
                                                getChat(),
                                                getOwner(),
                                                podcast
                                            )
                                        )
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
                    }
                }
            }
        }
    }


    override fun handleActivityResultUri(uri: Uri?) {
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
                            updateViewState(
                                TribeDetailViewState.TribeProfileUpdating
                            )
                            viewModelScope.launch(dispatchers.mainImmediate) {
                                val newFile: File = mediaCacheHandler.createImageFile(ext)

                                try {
                                    mediaCacheHandler.copyTo(stream, newFile)
                                    val repoResponse = chatRepository.updateChatProfilePic(
                                        getChat(),
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
                                            updateViewState(
                                                TribeDetailViewState.Tribe(
                                                    getChat(),
                                                    getOwner(),
                                                    podcast
                                                )
                                            )
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
