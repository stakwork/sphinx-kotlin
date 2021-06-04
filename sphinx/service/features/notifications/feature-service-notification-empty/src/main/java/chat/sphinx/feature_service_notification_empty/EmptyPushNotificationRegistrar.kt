package chat.sphinx.feature_service_notification_empty

import chat.sphinx.concept_service_notification.PushNotificationRegistrar
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d

internal class EmptyPushNotificationRegistrar(LOG: SphinxLogger): PushNotificationRegistrar() {
    override suspend fun register(): Response<Any, ResponseError> {
        return Response.Success(Any())
    }

    companion object {
        const val TAG = "EmptyPushNotificationRegistrar"
    }

    init {
        LOG.d(TAG, "Project compiled with empty notifications")
    }
}
