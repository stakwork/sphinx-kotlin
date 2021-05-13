package chat.sphinx.feature_service_notification_firebase

import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SphinxFirebaseMessagingService: FirebaseMessagingService() {

    companion object {
        const val TAG = "SphinxFirebaseMessagingService"
    }

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var dispatchers: CoroutineDispatchers

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var LOG: SphinxLogger

    private val supervisor: Job by lazy {
        SupervisorJob()
    }
    private val serviceScope: CoroutineScope by lazy {
        CoroutineScope(supervisor + dispatchers.mainImmediate)
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        LOG.d(TAG, "From: ${p0.from}")
        LOG.d(TAG, "Notification: ${p0.notification?.body}")
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        LOG.d(TAG, "TOKEN: $p0")
    }

    override fun onCreate() {
        super.onCreate()
        LOG.d(TAG, "onCreate")
    }
    override fun onDestroy() {
        super.onDestroy()
        supervisor.cancel()
        LOG.d(TAG, "onDestroy")
    }
}