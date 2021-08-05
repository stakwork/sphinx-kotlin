package chat.sphinx.feature_sphinx_service

import android.app.Service
import android.content.Intent
import androidx.annotation.CallSuper
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob


abstract class SphinxService: Service() {

    protected abstract val applicationServiceTracker: ApplicationServiceTracker
    protected abstract val dispatchers: CoroutineDispatchers
    protected abstract val mustComplete: Boolean

    private val supervisorJob: Job = SupervisorJob()
    protected val serviceLifecycleScope: CoroutineScope by lazy {
        CoroutineScope(supervisorJob + dispatchers.mainImmediate)
    }

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        applicationServiceTracker.onServiceCreated(mustComplete)
    }

    @CallSuper
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        applicationServiceTracker.onTaskRemoved()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        supervisorJob.cancel()
        applicationServiceTracker.onServiceDestroyed(mustComplete)
    }
}
