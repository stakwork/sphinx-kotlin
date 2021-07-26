package chat.sphinx.join_tribe.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.join_tribe.R
import chat.sphinx.join_tribe.navigation.JoinTribeNavigator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.toContactAlias
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.toMediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class JoinTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: JoinTribeNavigator,
    private val app: Application,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
    private val networkQueryChat: NetworkQueryChat,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
): SideEffectViewModel<
        Context,
        JoinTribeSideEffect,
        JoinTribeViewState
        >(dispatchers, JoinTribeViewState.LoadingTribe),
    PictureMenuViewModel
{
    private val args: JoinTribeFragmentArgs by savedStateHandle.navArgs()
    private var tribeInfo : TribeDto? = null

    fun setMyAlias(alias: String?) {
        this.tribeInfo?.myAlias = alias
    }

    private val _accountOwnerStateFlow: MutableStateFlow<Contact?> by lazy {
        MutableStateFlow(null)
    }

    override val pictureMenuHandler: PictureMenuHandler by lazy {
        PictureMenuHandler()
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
        if (tribeInfo != null) {
            viewStateContainer.updateViewState(JoinTribeViewState.TribeLoaded(tribeInfo!!))
            return
        }

        args.argTribeLink.toTribeJoinLink()?.let { tribeJoinLink ->
            viewModelScope.launch(mainImmediate) {
                networkQueryChat.getTribeInfo(ChatHost(tribeJoinLink.tribeHost), ChatUUID(tribeJoinLink.tribeUUID)).collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading ->
                            viewStateContainer.updateViewState(JoinTribeViewState.LoadingTribe)
                        is Response.Error -> {
                            submitSideEffect(JoinTribeSideEffect.Notify.InvalidTribe)
                            viewStateContainer.updateViewState(JoinTribeViewState.ErrorLoadingTribe)
                        }
                        is Response.Success -> {
                            if (loadResponse.value is TribeDto) {

                                tribeInfo = loadResponse.value
                                tribeInfo?.set(tribeJoinLink.tribeHost, tribeJoinLink.tribeUUID)

                                viewStateContainer.updateViewState(JoinTribeViewState.TribeLoaded(tribeInfo!!))
                            } else {
                                viewStateContainer.updateViewState(JoinTribeViewState.ErrorLoadingTribe)
                            }
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
            var tribeInfo = tribeInfo ?: let {
                submitSideEffect(JoinTribeSideEffect.Notify.InvalidTribe)
                return@launch
            }

            tribeInfo.myAlias = alias
            tribeInfo.amount = tribeInfo.price_to_join

            tribeInfo?.myAlias?.trim()?.toContactAlias() ?: let {
                submitSideEffect(JoinTribeSideEffect.Notify.AliasRequired)
                return@launch
            }

            chatRepository.joinTribe(tribeInfo).collect { loadResponse ->
                @Exhaustive
                when(loadResponse) {
                    LoadResponse.Loading ->
                        viewStateContainer.updateViewState(JoinTribeViewState.JoiningTribe)
                    is Response.Error -> {
                        submitSideEffect(JoinTribeSideEffect.Notify.ErrorJoining)
                        viewStateContainer.updateViewState(JoinTribeViewState.ErrorJoiningTribe)
                    }
                    is Response.Success ->
                        viewStateContainer.updateViewState(JoinTribeViewState.TribeJoined)
                }

            }
        }
    }

    private var cameraJob: Job? = null

    override fun updatePictureFromCamera() {
        if (cameraJob?.isActive == true) {
            return
        }

        cameraJob = viewModelScope.launch(dispatchers.mainImmediate) {
            pictureMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)

            val response = cameraCoordinator.submitRequest(CameraRequest)

            @Exhaustive
            when (response) {
                is Response.Error -> {
                    viewModelScope.launch(mainImmediate) {
                        submitSideEffect(JoinTribeSideEffect.Notify.FailedToProcessImage)
                    }
                }
                is Response.Success -> {
                    @Exhaustive
                    when (response.value) {
                        is CameraResponse.Image -> {
                            tribeInfo?.setProfileImageFile(response.value.value)

                            viewStateContainer.updateViewState(
                                JoinTribeViewState.TribeProfileImageUpdated(
                                    response.value.value
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun handleActivityResultUri(uri: Uri?) {
        uri?.let {
            val cr = app.contentResolver

            cr.getType(it)?.let { crType ->

                MimeTypeMap.getSingleton().getExtensionFromMimeType(crType)?.let { ext ->

                    crType.toMediaType().let { mType ->

                        @Exhaustive
                        when (mType) {
                            is MediaType.Image -> {
                                pictureMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)

                                if (it.path == null) {
                                    showFailedToProcessImage()
                                } else {
                                    try {
                                        cr.openInputStream(uri)?.let { inputStream ->
                                            val imageFile = File.createTempFile("sphinx", ".$ext", app.cacheDir)
                                            val outputStream = imageFile.outputStream()

                                            val buf = ByteArray(1024)
                                            while (true) {
                                                val read = inputStream.read(buf)
                                                if (read == -1) break
                                                outputStream.write(buf, 0, read)
                                            }

                                            tribeInfo?.setProfileImageFile(imageFile)

                                            viewStateContainer.updateViewState(
                                                JoinTribeViewState.TribeProfileImageUpdated(imageFile)
                                            )
                                        }
                                    } catch (e: Exception) {
                                        showFailedToProcessImage()
                                    }
                                }
                            }
                            is MediaType.Audio,
                            is MediaType.Pdf,
                            is MediaType.Text,
                            is MediaType.Unknown,
                            is MediaType.Video -> {
                                showFailedToProcessImage()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showFailedToProcessImage() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(JoinTribeSideEffect.Notify.FailedToProcessImage)
        }
    }
}
