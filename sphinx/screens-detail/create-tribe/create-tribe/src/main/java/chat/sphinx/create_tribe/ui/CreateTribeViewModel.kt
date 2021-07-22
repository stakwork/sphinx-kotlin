package chat.sphinx.create_tribe.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
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
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_tribe_pic.TribePicMenuHandler
import chat.sphinx.menu_bottom_tribe_pic.TribePicMenuViewModel
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.toMediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class CreateTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val app: Application,
    val chatRepository: ChatRepository,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    val navigator: CreateTribeNavigator,
): SideEffectViewModel<
        Context,
        CreateTribeSideEffect,
        CreateTribeViewState>(dispatchers, CreateTribeViewState.Idle),
    TribePicMenuViewModel
{
    val createTribeBuilder = CreateTribe.Builder()

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_media_library)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    override val tribePicMenuHandler: TribePicMenuHandler by lazy {
        TribePicMenuHandler()
    }

    private var cameraJob: Job? = null


    override fun updateProfilePicCamera() {
        if (cameraJob?.isActive == true) {
            return
        }

        cameraJob = viewModelScope.launch(dispatchers.mainImmediate) {
            val response = cameraCoordinator.submitRequest(CameraRequest)

            @Exhaustive
            when (response) {
                is Response.Error -> {
                    viewModelScope.launch(mainImmediate) {
                        submitSideEffect(CreateTribeSideEffect.FailedToProcessImage)
                    }
                }
                is Response.Success -> {
                    tribePicMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
                    @Exhaustive
                    when (response.value) {
                        is CameraResponse.Image -> {
                            createTribeBuilder.setImg(response.value.value)
                            updateViewState(
                                CreateTribeViewState.TribeImageUpdated(response.value.value)
                            )
                            // TODO: Update image...
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
                                tribePicMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)

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

                                            createTribeBuilder.setImg(imageFile)
                                            updateViewState(
                                                CreateTribeViewState.TribeImageUpdated(imageFile)
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
                                viewModelScope.launch(mainImmediate) {
                                    submitSideEffect(CreateTribeSideEffect.FailedToProcessImage)
                                }
                            }
                        }
                    }
                }

            }
        }


    }

    private fun showNameAndDescriptionRequired() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(CreateTribeSideEffect.NameAndDescriptionRequired)
        }
    }

    private fun showFailedToProcessImage() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(CreateTribeSideEffect.FailedToProcessImage)
        }
    }

    fun createTribe() {
        if (createTribeBuilder.hasRequiredFields) {
            createTribeBuilder.build()?.let {
                updateViewState(CreateTribeViewState.CreatingTribe)
                viewModelScope.launch(mainImmediate) {
                    when(chatRepository.createTribe(it)) {
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
            showNameAndDescriptionRequired()
        }
    }
}
