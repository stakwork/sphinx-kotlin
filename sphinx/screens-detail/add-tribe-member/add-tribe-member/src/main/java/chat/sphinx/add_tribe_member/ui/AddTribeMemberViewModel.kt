package chat.sphinx.add_tribe_member.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.add_tribe_member.R
import chat.sphinx.add_tribe_member.navigation.AddTribeMemberNavigator
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_chat.model.AddMember
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import chat.sphinx.wrapper_common.dashboard.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val AddTribeMemberFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

@HiltViewModel
internal class AddTribeMemberViewModel @Inject constructor(
    private val app: Application,
    dispatchers: CoroutineDispatchers,
    val navigator: AddTribeMemberNavigator,
    private val chatRepository: ChatRepository,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    private val mediaCacheHandler: MediaCacheHandler,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        AddTribeMemberSideEffect,
        AddTribeMemberViewState
        >(dispatchers, AddTribeMemberViewState.Idle),
    PictureMenuViewModel
{
    private val args: AddTribeMemberFragmentArgs by savedStateHandle.navArgs()
    private val chatId: ChatId? = args.chatId

    val addTribeMemberBuilder = AddMember.Builder()

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_media_library)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    init {
        addTribeMemberBuilder.setChatId(chatId?.value)
    }

    private var addMemberJob: Job? = null
    fun addTribeMember() {
        if (addMemberJob?.isActive == true) {
            return
        }

        val buildPair = addTribeMemberBuilder.build()

        buildPair?.second?.let {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    when (it) {
                        AddMember.ValidationError.MISSING_FIELDS -> {
                            AddTribeMemberSideEffect.FieldsRequired
                        }
                        AddMember.ValidationError.INVALID_PUBLIC_KEY -> {
                            AddTribeMemberSideEffect.InvalidPublicKey
                        }
                        AddMember.ValidationError.INVALID_ROUTE_HINT -> {
                            AddTribeMemberSideEffect.InvalidRouteHint
                        }
                    }
                )
            }
            return
        }

        buildPair.first?.let {
            updateViewState(AddTribeMemberViewState.SavingMember)

            addMemberJob = viewModelScope.launch(mainImmediate) {
                when(chatRepository.addTribeMember(it)) {
                    is Response.Error -> {
                        submitSideEffect(AddTribeMemberSideEffect.FailedToAddMember)
                    }
                    is Response.Success -> {
                        navigator.popBackStack()
                    }
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
                        addTribeMemberBuilder.setImg(imageFile)
                        updateViewState(AddTribeMemberViewState.MemberImageUpdated(imageFile))
                    } else {
                        submitSideEffect(AddTribeMemberSideEffect.FailedToProcessImage)
                    }
                }
            }
        )
    }
}
