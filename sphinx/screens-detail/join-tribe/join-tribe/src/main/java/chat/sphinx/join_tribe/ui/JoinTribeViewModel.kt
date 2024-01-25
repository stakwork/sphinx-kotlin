package chat.sphinx.join_tribe.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.NewTribeDto
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_connect_manager.ConnectManagerRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.join_tribe.R
import chat.sphinx.join_tribe.navigation.JoinTribeNavigator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_chat.toChatHost
import chat.sphinx.wrapper_common.chat.toChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.toFeedUrl
import chat.sphinx.wrapper_common.feed.toSubscribed
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.toContactAlias
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class JoinTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: JoinTribeNavigator,
    private val app: Application,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
    private val feedRepository: FeedRepository,
    private val networkQueryChat: NetworkQueryChat,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    private val mediaCacheHandler: MediaCacheHandler,
    private val connectManagerRepository: ConnectManagerRepository,
    ): SideEffectViewModel<
        Context,
        JoinTribeSideEffect,
        JoinTribeViewState
        >(dispatchers, JoinTribeViewState.LoadingTribe),
    PictureMenuViewModel
{
    private val args: JoinTribeFragmentArgs by savedStateHandle.navArgs()
//    private var tribeInfo : TribeDto? = null
    private var newTribeInfo : NewTribeDto? = null

    fun setMyAlias(alias: String?) {
        this.newTribeInfo?.myAlias = alias
    }

    private val _accountOwnerStateFlow: MutableStateFlow<Contact?> by lazy {
        MutableStateFlow(null)
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

                        // if the file does not exist, create a temporary file in the
                        // image cache directory
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
                        newTribeInfo?.setProfileImageFile(imageFile)
                        updateViewState(JoinTribeViewState.TribeProfileImageUpdated(imageFile))
                    } else {
                        submitSideEffect(JoinTribeSideEffect.Notify.FailedToProcessImage)
                    }
                }
            }
        )
    }

    val accountOwnerStateFlow: StateFlow<Contact?>
        get() = _accountOwnerStateFlow.asStateFlow()

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    init {
        viewModelScope.launch(mainImmediate) {
            contactRepository.accountOwner.collect { contact ->
                _accountOwnerStateFlow.value = contact
            }
        }
    }

    fun loadTribeData() {
        newTribeInfo?.let { nnTribeInfo ->
//            viewStateContainer.updateViewState(JoinTribeViewState.TribeLoaded(nnTribeInfo))
            return
        }

        args.argTribeLink.toTribeJoinLink()?.let { tribeJoinLink ->
            viewModelScope.launch(mainImmediate) {

                networkQueryChat.getTribeInfo(ChatHost(tribeJoinLink.tribeHost), LightningNodePubKey(tribeJoinLink.tribePubkey)).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading ->
                            viewStateContainer.updateViewState(JoinTribeViewState.LoadingTribe)
                        is Response.Error -> {
                            submitSideEffect(JoinTribeSideEffect.Notify.InvalidTribe)
                            viewStateContainer.updateViewState(JoinTribeViewState.ErrorLoadingTribe)
                        }
                        is Response.Success -> {

                            newTribeInfo = loadResponse.value
                            newTribeInfo?.set(tribeJoinLink.tribeHost, tribeJoinLink.tribePubkey)

                            val tribeLoaded = JoinTribeViewState.TribeLoaded(
                                loadResponse.value.name,
                                "",
                                null,
                                "1",
                                "0",
                                "0",
                                "0",
                                accountOwnerStateFlow.value?.alias?.value,
                                accountOwnerStateFlow.value?.photoUrl?.value
                            )
                            updateViewState(tribeLoaded)
                        }
                    }
                }
            }
        } ?: run {
            viewStateContainer.updateViewState(JoinTribeViewState.ErrorLoadingTribe)
        }
    }

    fun joinTribe(alias: String) {
        viewModelScope.launch(mainImmediate) {
            val tribeInfo = newTribeInfo ?: let {
                submitSideEffect(JoinTribeSideEffect.Notify.InvalidTribe)
                return@launch
            }

            tribeInfo.myAlias = alias
            tribeInfo.amount = tribeInfo.price_to_join

            tribeInfo.myAlias?.trim()?.toContactAlias() ?: let {
                submitSideEffect(JoinTribeSideEffect.Notify.AliasRequired)
                return@launch
            }

            val host = tribeInfo.host

            if (host != null) {
                connectManagerRepository.joinTribe(
                    host,
                    tribeInfo.pubkey,
                    tribeInfo.route_hint
                )
                updateViewState(JoinTribeViewState.TribeJoined)
            } else {
                submitSideEffect(JoinTribeSideEffect.Notify.ErrorJoining)
                updateViewState(JoinTribeViewState.ErrorJoiningTribe)
            }

//            chatRepository.joinTribe(tribeInfo).collect { loadResponse ->
//                @Exhaustive
//                when(loadResponse) {
//                    LoadResponse.Loading ->
//                        updateViewState(JoinTribeViewState.JoiningTribe)
//                    is Response.Error -> {
//                        submitSideEffect(
//                            JoinTribeSideEffect.Notify.ErrorJoining
//                        )
//                        updateViewState(JoinTribeViewState.ErrorJoiningTribe)
//                    }
//                    is Response.Success -> {
//                        updateFeedContent(
//                            ChatId(loadResponse.value.id)
//                        )
//                        updateViewState(JoinTribeViewState.TribeJoined)
//                    }
//                }
//            }
        }
    }

    private suspend fun updateFeedContent(chatId: ChatId) {
        // Needs to implement

//        newTribeInfo?.let { nnTribeInfo ->
//            nnTribeInfo.feed_url?.toFeedUrl()?.let { feedUrl ->
//                nnTribeInfo.uuid?.toChatUUID()?.let { chatUUID ->
//                    nnTribeInfo.host?.toChatHost()?.let { chatHost ->
//                        feedRepository.updateFeedContent(
//                            chatId = chatId,
//                            host = chatHost,
//                            feedUrl = feedUrl,
//                            chatUUID = chatUUID,
//                            subscribed = false.toSubscribed(),
//                            currentItemId = null
//                        )
//                    }
//                }
//            }
//        }
    }
}
