package chat.sphinx.camera.di

import chat.sphinx.camera.coordinator.CameraViewModelCoordinator
import chat.sphinx.camera.navigation.CameraNavigator
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.logger.SphinxLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object CameraModule {

    @Provides
    @ActivityRetainedScoped
    fun provideCameraCoordinator(
        dispatchers: CoroutineDispatchers,
        cameraNavigator: CameraNavigator,
        LOG: SphinxLogger,
    ): CameraViewModelCoordinator =
        CameraViewModelCoordinator(dispatchers, cameraNavigator, LOG)

    @Provides
    fun provideViewModelCoordinator(
        cameraViewModelCoordinator: CameraViewModelCoordinator
    ): ViewModelCoordinator<CameraRequest, CameraResponse> =
        cameraViewModelCoordinator
}
