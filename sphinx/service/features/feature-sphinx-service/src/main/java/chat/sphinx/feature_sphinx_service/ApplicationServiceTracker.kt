package chat.sphinx.feature_sphinx_service

import androidx.annotation.MainThread

abstract class ApplicationServiceTracker {

    @Volatile
    var mustCompleteServicesCounter: Int = 0
        private set

    @MainThread
    fun onServiceCreated(mustComplete: Boolean) {
        if (mustComplete) {
            mustCompleteServicesCounter++
        }
    }

    @MainThread
    open fun onServiceDestroyed(mustComplete: Boolean) {
        if (mustComplete) {
            mustCompleteServicesCounter--
        }
    }

    @Volatile
    var taskIsRemoved: Boolean = false
        private set

    @MainThread
    fun taskReturned() {
        taskIsRemoved = false
    }

    @MainThread
    open fun onTaskRemoved() {
        if (!taskIsRemoved) {
            taskIsRemoved = true
        }
    }
}
