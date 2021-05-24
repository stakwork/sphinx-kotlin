package chat.sphinx.feature_service_notification_firebase

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_service_notification.PushNotificationRegistrar
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.w
import chat.sphinx.wrapper_contact.DeviceId
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect

internal class FirebasePushNotificationRegistrar(
    private val contactRepository: ContactRepository,
    private val LOG: SphinxLogger,
): PushNotificationRegistrar() {

    companion object {
        const val TAG = "FirebasePushNotificationRegistrar"
    }

    override suspend fun register(): Response<Any, ResponseError> {
        val tokenFetchResponse: MutableStateFlow<Response<String, ResponseError>?> =
            MutableStateFlow(null)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                val msg = "Fetching FCM registration token failed"
                LOG.w(TAG, msg, task.exception)
                tokenFetchResponse.value = Response.Error(ResponseError(msg, task.exception))
                return@OnCompleteListener
            }

            val token = task.result

            tokenFetchResponse.value = if (token != null) {
                LOG.d(TAG, "FCM token: $token")
                Response.Success(token)
            } else {
                val msg = "Fetching FCM registration token succeeded, but token was null"
                LOG.w(TAG, msg, task.exception)
                Response.Error(ResponseError(msg, task.exception))
            }
        })

        try {
            // need to wait for the listener to complete
            tokenFetchResponse.collect { response ->
                @Exhaustive
                when (response) {
                    null -> {}
                    is Response.Error,
                    is Response.Success -> {
                        throw Exception()
                    }
                }
            }
        } catch (e: Exception) {}

        if (tokenFetchResponse.value is Response.Error) {
            return tokenFetchResponse.value!!
        }

        return contactRepository.updateOwnerDeviceId(
            DeviceId(
                (tokenFetchResponse.value!! as Response.Success).value
            )
        )
    }

    init {
        LOG.d(TAG, "Project compiled with Firebase notifications")
    }
}
