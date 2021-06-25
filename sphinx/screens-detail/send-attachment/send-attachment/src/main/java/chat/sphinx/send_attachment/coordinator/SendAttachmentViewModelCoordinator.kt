package chat.sphinx.send_attachment.coordinator

import chat.sphinx.concept_view_model_coordinator.RequestHolder
import chat.sphinx.feature_view_model_coordinator.ViewModelCoordinatorImpl
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.send_attachment.navigation.SendAttachmentNavigator
import chat.sphinx.send_attachment_view_model_coordinator.request.SendAttachmentRequest
import chat.sphinx.send_attachment_view_model_coordinator.response.SendAttachmentResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class SendAttachmentViewModelCoordinator(
    dispatchers: CoroutineDispatchers,
    private val sendAttachmentNavigator: SendAttachmentNavigator,
    LOG: SphinxLogger,
): ViewModelCoordinatorImpl<Any, SendAttachmentRequest, SendAttachmentResponse>(
    LOG = LOG,
    dispatcher = dispatchers.mainImmediate
) {
    override suspend fun navigateBack(back: Any) {
        sendAttachmentNavigator.popBackStack()
    }

    override suspend fun navigateToScreen(request: RequestHolder<SendAttachmentRequest>) {
        sendAttachmentNavigator.toSendAttachmentScreen(request.request.isConversation)
    }

    override suspend fun checkRequest(request: SendAttachmentRequest): SendAttachmentResponse? {
        return null
    }
}
