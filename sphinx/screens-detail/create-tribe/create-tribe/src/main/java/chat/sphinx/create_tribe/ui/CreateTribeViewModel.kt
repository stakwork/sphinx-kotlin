package chat.sphinx.create_tribe.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_chat.model.CreateTribe
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.create_tribe.R
import chat.sphinx.create_tribe.navigation.CreateTribeNavigator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.launch
import app.cash.exhaustive.Exhaustive
import kotlinx.coroutines.Job
import javax.inject.Inject

internal inline val CreateTribeFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

@HiltViewModel
internal class CreateTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    savedStateHandle: SavedStateHandle,
    private val networkQueryChat: NetworkQueryChat,
    private val chatRepository: ChatRepository,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    private val mediaCacheHandler: MediaCacheHandler,
    val navigator: CreateTribeNavigator,
): SideEffectViewModel<
        Context,
        CreateTribeSideEffect,
        CreateTribeViewState>(dispatchers, CreateTribeViewState.Idle),
    PictureMenuViewModel
{
    private val args: CreateTribeFragmentArgs by savedStateHandle.navArgs()
    private val chatId: ChatId? = args.chatId

    private val chatSharedFlow: SharedFlow<Chat?> = flow {
        chatId?.let {
            emitAll(chatRepository.getChatById(it))
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private suspend fun getChat(): Chat? {
        return chatId?.let {
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
            return chat
        }
    }

    val createTribeBuilder = CreateTribe.Builder(
        arrayOf(
            CreateTribe.Builder.Tag("Bitcoin", R.drawable.ic_bitcoin),
            CreateTribe.Builder.Tag("Lightning", R.drawable.ic_lightning),
            CreateTribe.Builder.Tag("Sphinx", R.drawable.ic_sphinx),
            CreateTribe.Builder.Tag("Crypto", R.drawable.ic_crypto),
            CreateTribe.Builder.Tag("Tech", R.drawable.ic_tech),
            CreateTribe.Builder.Tag("Altcoins", R.drawable.ic_altcoins),
            CreateTribe.Builder.Tag("Music", R.drawable.ic_music),
            CreateTribe.Builder.Tag("Pod", R.drawable.ic_podcast),
        )
    )

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_media_library)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    fun headerText(): String {
        return if (chatId == null) {
            app.getString(R.string.create_tribe_header_name)
        } else {
            app.getString(R.string.edit_tribe_header_name)
        }
    }

    fun submitButtonText(): String {
        return if (chatId == null) {
            app.getString(R.string.create_tribe)
        } else {
            app.getString(R.string.save)
        }
    }

    fun load() {
        viewModelScope.launch(mainImmediate) {
            getChat()?.let { chat ->
                updateViewState(CreateTribeViewState.LoadingExistingTribe)
                val host = chat.host
                if (host != null) {
                    networkQueryChat.getTribeInfo(host, chat.uuid).collect { loadResponse ->
                        when (loadResponse) {
                            is LoadResponse.Loading -> {}
                            is Response.Error -> {
                                submitSideEffect(CreateTribeSideEffect.FailedToLoadTribe)
                                navigator.closeDetailScreen()
                            }
                            is Response.Success -> {
                                createTribeBuilder.load(loadResponse.value)
                                updateViewState(CreateTribeViewState.ExistingTribe)
                            }
                        }
                    }
                } else {
                    submitSideEffect(CreateTribeSideEffect.FailedToLoadTribe)
                    navigator.closeDetailScreen()
                }

            }
        }
    }

    override val pictureMenuHandler: PictureMenuHandler by lazy {
        PictureMenuHandler(
            app = app,
            cameraCoordinator = cameraCoordinator,
            dispatchers = this,
            viewModel = this,
            callback = { streamProvider, _, fileName, _, file ->
                viewModelScope.launch(mainImmediate) {
                    val imageFile = if (file != null) {
                        file
                    } else {

                        val newFile = mediaCacheHandler.createImageFile(
                            fileName.split(".").last()
                        )

                        try {
                            mediaCacheHandler.copyTo(
                                from = streamProvider.newInputStream(),
                                to = newFile
                            )
                            newFile
                        } catch (e: Exception) {
                            newFile.delete()
                            null
                        }
                    }

                    if (imageFile != null) {
                        createTribeBuilder.setImg(imageFile)
                        updateViewState(CreateTribeViewState.TribeImageUpdated(imageFile))
                    } else {
                        submitSideEffect(CreateTribeSideEffect.FailedToProcessImage)
                    }
                }
            }
        )
    }

    fun saveTribe() {
        if (createTribeBuilder.hasRequiredFields) {
            createTribeBuilder.build()?.let {
                updateViewState(CreateTribeViewState.SavingTribe)
                viewModelScope.launch(mainImmediate) {
                    if (chatId == null) {
                        when(chatRepository.createTribe(it)) {
                            is Response.Error -> {
                                submitSideEffect(CreateTribeSideEffect.FailedToCreateTribe)
                            }
                            is Response.Success -> {
                                navigator.closeDetailScreen()
                            }
                        }
                    } else {
                        when(chatRepository.updateTribe(chatId!!, it)) {
                            is Response.Error -> {
                                submitSideEffect(CreateTribeSideEffect.FailedToUpdateTribe)
                            }
                            is Response.Success -> {
                                navigator.closeDetailScreen()
                            }
                        }
                    }

                }

            }
        } else {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(CreateTribeSideEffect.NameAndDescriptionRequired)
            }
        }
    }

    fun selectTags(callback: () -> Unit) {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(
                CreateTribeSideEffect.AlertSelectTags(createTribeBuilder, callback)
            )
        }
    }
}
