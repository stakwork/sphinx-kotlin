package chat.sphinx.feature_service_notification_firebase

import android.app.Application
import android.content.Context
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
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow

internal class FirebasePushNotificationRegistrar(
    private val contactRepository: ContactRepository,
    private val LOG: SphinxLogger,
    private val app: Context
): PushNotificationRegistrar() {

    companion object {
        const val TAG = "FirebasePushNotificationRegistrar"
    }
    override suspend fun register(): Response<Any, ResponseError> {
        val tokenFetchResponse: MutableStateFlow<Response<String, ResponseError>?> =
            MutableStateFlow(null)

        try {
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
        }
        catch (e: Exception) {
            val msg = "Unexpected error occurred while fetching the Firebase Cloud Messaging registration token"
            LOG.w(TAG, msg, e)
        }

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

        return tokenFetchResponse.value?.let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    response
                }
                is Response.Success -> {
                    contactRepository.updateOwnerDeviceId(DeviceId(response.value))
                }
            }
        } ?: Response.Error(ResponseError("NotificationToken retrieved was null"))
    }

    init {
        try {
            FirebaseApp.initializeApp(app.applicationContext)
        } catch (e: IllegalStateException) {
            val msg = "Default FirebaseApp is not initialized in this process..."
            LOG.w(TAG, msg, e)
        }
        LOG.d(TAG, "Project compiled with Firebase notifications")
    }
}
