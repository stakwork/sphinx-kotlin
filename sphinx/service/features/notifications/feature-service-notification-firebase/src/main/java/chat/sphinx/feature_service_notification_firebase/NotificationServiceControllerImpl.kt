package chat.sphinx.feature_service_notification_firebase

import chat.sphinx.concept_service_notification.NotificationServiceController
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d

class NotificationServiceControllerImpl(
    private val LOG: SphinxLogger,
): NotificationServiceController() {

    companion object {
        const val TAG = "NotificationServiceControllerImpl"
    }

    override suspend fun register(): Response<Any, ResponseError> {
        return Response.Success(Any())
    }

    init {
        LOG.d(TAG, "Project compiled with Firebase notifications")
    }
}
