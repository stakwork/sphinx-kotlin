package chat.sphinx.create_tribe.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_chat.model.CreateTribe
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.create_tribe.R
import chat.sphinx.create_tribe.navigation.CreateTribeNavigator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.launch
import app.cash.exhaustive.Exhaustive
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
internal class CreateTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
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

    private var createTribeJob: Job? = null
    fun createTribe() {
        if (createTribeJob?.isActive == true) {
            return
        }

        if (createTribeBuilder.hasRequiredFields) {
            createTribeBuilder.build()?.let { createTribe ->

                updateViewState(CreateTribeViewState.CreatingTribe)

                createTribeJob = viewModelScope.launch(mainImmediate) {
                    val response = chatRepository.createTribe(createTribe)

                    @Exhaustive
                    when(response) {
                        is Response.Error -> {
                            submitSideEffect(CreateTribeSideEffect.FailedToCreateTribe)
                        }
                        is Response.Success -> {
                            navigator.closeDetailScreen()
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
