package chat.sphinx.concept_service_media

import chat.sphinx.wrapper_common.ItemId

/**
 * This class is a service object to facilitate bi-directional communications
 * between the UI, and MediaPlayerService without leaking the services context. It
 * is utilized as a Singleton.
 *
 * Care needs to be taken when attaching a listener to receive [MediaPlayerServiceState]
 * updates, such that it is removed prior to the end of the listener object's life cycle.
 * */
abstract class MediaPlayerServiceController {

    protected abstract fun getCurrentState(): MediaPlayerServiceState

    protected inner class ListenerHandler {
        private val listeners: MutableSet<MediaServiceListener> = LinkedHashSet(3)

        @Synchronized
        fun dispatch(mediaPlayerServiceState: MediaPlayerServiceState) {
            for (listener in listeners) {
                listener.mediaServiceState(mediaPlayerServiceState)
            }
        }

        @Synchronized
        fun add(listener: MediaServiceListener) {
            if (listeners.add(listener)) {
                listener.mediaServiceState(getCurrentState())
            }
        }

        @Synchronized
        fun remove(listener: MediaServiceListener) {
            listeners.remove(listener)
        }
    }

    protected val listenerHandler: ListenerHandler by lazy {
        ListenerHandler()
    }

    fun addListener(listener: MediaServiceListener) {
        listenerHandler.add(listener)
    }

    fun removeListener(listener: MediaServiceListener) {
        listenerHandler.remove(listener)
    }

    interface MediaServiceListener {

        /**
         * Registration of the listener for the first time (if it is not already
         * registered), will have the current [MediaPlayerServiceState] dispatched.
         * */
        fun mediaServiceState(serviceState: MediaPlayerServiceState)
    }

    abstract suspend fun submitAction(userAction: UserAction)

    abstract fun getPlayingContent(): Triple<String, String, Boolean>?
}
