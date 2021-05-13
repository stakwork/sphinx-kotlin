package chat.sphinx.concept_service_notification

import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError

abstract class NotificationServiceController {
    abstract suspend fun register(): Response<Any, ResponseError>
}
