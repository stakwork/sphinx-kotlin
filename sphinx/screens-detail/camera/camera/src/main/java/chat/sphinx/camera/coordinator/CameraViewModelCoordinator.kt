package chat.sphinx.camera.coordinator

import chat.sphinx.camera.navigation.CameraNavigator
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_view_model_coordinator.RequestHolder
import chat.sphinx.feature_view_model_coordinator.ViewModelCoordinatorImpl
import chat.sphinx.logger.SphinxLogger
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class CameraViewModelCoordinator(
    dispatchers: CoroutineDispatchers,
    private val cameraNavigator: CameraNavigator,
    LOG: SphinxLogger,
): ViewModelCoordinatorImpl<Any, CameraRequest, CameraResponse>(
    LOG = LOG,
    dispatcher = dispatchers.mainImmediate,
) {
    override suspend fun navigateBack(back: Any) {
        cameraNavigator.popBackStack()
    }

    override suspend fun navigateToScreen(request: RequestHolder<CameraRequest>) {
        cameraNavigator.toCameraDetailScreen()
    }

    override suspend fun checkRequest(request: CameraRequest): CameraResponse? {
        return null
    }
}
