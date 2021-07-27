package chat.sphinx.onboard_picture.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import chat.sphinx.onboard_picture.navigation.OnBoardPictureNavigator
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class OnBoardPictureViewModel @Inject constructor(
    app: Application,
    private val contactRepository: ContactRepository,
    cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    dispatchers: CoroutineDispatchers,
    private val navigator: OnBoardPictureNavigator,
    handle: SavedStateHandle,
) : SideEffectViewModel<
        Context,
        OnBoardPictureSideEffect,
        OnBoardPictureViewState
        >(dispatchers, OnBoardPictureViewState.Idle),
    PictureMenuViewModel
{
    val args: OnBoardPictureFragmentArgs by handle.navArgs()

    private val refreshJob: Job? = if (args.argRefreshContacts) {
        viewModelScope.launch(mainImmediate) {
            contactRepository.networkRefreshContacts.collect { response ->
                @Exhaustive
                when (response) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {}
                    is Response.Success -> {}
                }
            }
        }
    } else {
        null
    }

    fun nextScreen() {

    }

    override val pictureMenuHandler: PictureMenuHandler by lazy {
        PictureMenuHandler(
            app = app,
            cameraCoordinator = cameraCoordinator,
            dispatchers = this,
            viewModel = this,
            callback = { streamProvider, mediaType, fileName, contentLength, file ->
                // TODO: Show Progress Bar

                viewModelScope.launch(mainImmediate) {
                    refreshJob?.join()

                    val response = contactRepository.updateProfilePic(
                        stream = streamProvider,
                        mediaType = mediaType,
                        fileName = fileName,
                        contentLength = contentLength,
                    )

                    @Exhaustive
                    when (response) {
                        is Response.Error -> {
                            // TODO: Handle Error
                        }
                        is Response.Success -> {}
                    }

                    // TODO: Hide Progress Bar

                    try {
                        file?.delete()
                    } catch (e: Exception) {}
                }
            }
        )
    }

    private inner class OwnerViewStateContainer: ViewStateContainer<OnBoardPictureViewState>(OnBoardPictureViewState.Idle) {
        override val viewStateFlow: StateFlow<OnBoardPictureViewState> by lazy {
            flow<OnBoardPictureViewState> {
                contactRepository.accountOwner.collect { owner ->
                    emit(
                        if (owner != null) {
                            OnBoardPictureViewState.UserInfo(owner.alias, owner.photoUrl)
                        } else {
                            OnBoardPictureViewState.Idle
                        }
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = OnBoardPictureViewState.Idle
            )
        }
    }

    override val viewStateContainer: ViewStateContainer<OnBoardPictureViewState> by lazy {
        OwnerViewStateContainer()
    }
}
