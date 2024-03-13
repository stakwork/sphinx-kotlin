package chat.sphinx.tribe_detail.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import chat.sphinx.menu_bottom_profile_pic.UpdatingImageViewState
import chat.sphinx.tribe.TribeMenuHandler
import chat.sphinx.tribe.TribeMenuViewModel
import chat.sphinx.tribe_detail.R
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatAlias
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_feed.Feed.Companion.TRIBES_DEFAULT_SERVER_URL
import chat.sphinx.wrapper_meme_server.PublicAttachmentInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    val navigator: TribeDetailNavigator,
    val LOG: SphinxLogger,
): SideEffectViewModel<
        Context,
        TribeDetailSideEffect,
        TribeDetailViewState>(dispatchers, TribeDetailViewState.Idle),
    TribeMenuViewModel,
    PictureMenuViewModel
{
    companion object {
        const val TAG = "TribeDetailViewModel"
        private const val TRIBES_DEFAULT_SERVER_URL = "34.229.52.200:8801"
    }

    private val args: TribeDetailFragmentArgs by savedStateHandle.navArgs()

    val chatId = args.chatId

    val updatingImageViewStateContainer: ViewStateContainer<UpdatingImageViewState> by lazy {
        ViewStateContainer(UpdatingImageViewState.Idle)
    }

    private val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )


    private inner class TribeDetailViewStateContainer: ViewStateContainer<TribeDetailViewState>(TribeDetailViewState.Idle) {
        override val viewStateFlow: StateFlow<TribeDetailViewState> by lazy {
            flow {
                chatSharedFlow.collect { chat ->
                    emit(
                        if (chat != null) {
                            TribeDetailViewState.TribeProfile(
                                chat,
                                getOwner(),
                            )
                        } else {
                            TribeDetailViewState.Idle
                        }
                    )
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TribeDetailViewState.Idle,
            )
        }
    }

    override val viewStateContainer: ViewStateContainer<TribeDetailViewState> by lazy {
        TribeDetailViewStateContainer()
    }

    suspend fun getOwner(): Contact {
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

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    override val tribeMenuHandler: TribeMenuHandler by lazy {
        TribeMenuHandler()
    }

    override val pictureMenuHandler: PictureMenuHandler by lazy {
        PictureMenuHandler(
            app = app,
            cameraCoordinator = cameraCoordinator,
            dispatchers = this,
            viewModel = this,
            callback = { streamProvider, mediaType, fileName, contentLength, file ->
                updatingImageViewStateContainer.updateViewState(
                    UpdatingImageViewState.UpdatingImage
                )

                viewModelScope.launch(mainImmediate) {
                    try {
                        val attachmentInfo = PublicAttachmentInfo(
                            stream = streamProvider,
                            mediaType = mediaType,
                            fileName = fileName,
                            contentLength = contentLength
                        )

                        val response = chatRepository.updateChatProfileInfo(
                            chatId = ChatId(args.argChatId),
                            alias = null,
                            attachmentInfo,
                        )

                        @Exhaustive
                        when (response) {
                            is Response.Error -> {
                                LOG.e(TAG, "Error update chat Profile Picture: ", response.cause.exception)

                                updatingImageViewStateContainer.updateViewState(
                                    UpdatingImageViewState.UpdatingImageFailed
                                )

                                submitSideEffect(TribeDetailSideEffect.FailedToUpdateProfilePic)
                            }
                            is Response.Success -> {
                                updatingImageViewStateContainer.updateViewState(
                                    UpdatingImageViewState.UpdatingImageSucceed
                                )
                            }
                        }
                    } catch (e: Exception) {
                        updatingImageViewStateContainer.updateViewState(
                            UpdatingImageViewState.UpdatingImageFailed
                        )

                        submitSideEffect(TribeDetailSideEffect.FailedToUpdateProfilePic)
                    }

                    try {
                        file?.delete()
                    } catch (e: Exception) {}
                }
            }
        )
    }
    fun goToTribeBadgesScreen() {
        viewModelScope.launch(mainImmediate) {
            navigator.toTribeBadgesScreen(chatId)
        }
    }

    fun updateProfileAlias(alias: String?) {
        viewModelScope.launch(mainImmediate) {
            val response = chatRepository.updateChatProfileInfo(
                ChatId(args.argChatId),
                alias?.let { ChatAlias(it) }
            )

            when (response) {
                is Response.Success -> {}
                is Response.Error -> {
                    submitSideEffect(TribeDetailSideEffect.FailedToUpdateProfileAlias)
                }
            }
        }
    }

    /***
     * Tribe Menu Implementation
     */
    override fun deleteTribe() {
        viewModelScope.launch(mainImmediate) {
            val chat = getChat()

            submitSideEffect(
                TribeDetailSideEffect.AlertConfirmDeleteTribe(chat) {
                    viewModelScope.launch(mainImmediate) {

                        chatRepository.exitAndDeleteTribe(chat)
                        navigator.goBackToDashboard()

                    }
                }
            )
        }
        tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
    }

    override fun shareTribe() {
        viewModelScope.launch(mainImmediate) {
            val chat = getChat()
            if (chat.isTribeOwnedByAccount(getOwner().nodePubKey)) {
                val shareTribeURL = "sphinx.chat://?action=tribeV2&pubkey=${chat.uuid.value}&host=${TRIBES_DEFAULT_SERVER_URL}"
                navigator.toShareTribeScreen(shareTribeURL, app.getString(R.string.qr_code_title))
            }
        }

        tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
    }

    override fun exitTribe() {
        viewModelScope.launch(mainImmediate) {
            val chat = getChat()

            submitSideEffect(
                TribeDetailSideEffect.AlertConfirmExitTribe(chat) {
                    viewModelScope.launch(mainImmediate) {
                        chatRepository.exitAndDeleteTribe(chat)
                        navigator.goBackToDashboard()
                    }
                }
            )
        }
        tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
    }

    override fun editTribe() {
        tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)

        viewModelScope.launch(mainImmediate) {
            navigator.toEditTribeScreen(chatId)
        }
    }

    override fun addTribeMember() {
        tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
        viewModelScope.launch(mainImmediate) {
            navigator.toAddMember(chatId)
        }
    }

    fun toTribeMemberList() {
        viewModelScope.launch(mainImmediate) {
            navigator.toTribeMemberList(chatId)
        }
    }
}
