package chat.sphinx.feature_service_media_player_android

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.service.MediaPlayerService
import chat.sphinx.feature_service_media_player_android.service.SphinxMediaPlayerService
import chat.sphinx.feature_service_media_player_android.util.toIntent
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class MediaPlayerServiceControllerImpl(
    private val app: Application,
    dispatchers: CoroutineDispatchers,
    private val repositoryMedia: RepositoryMedia,
): MediaPlayerServiceController(), CoroutineDispatchers by dispatchers {

    private val binder: MutableStateFlow<MediaPlayerService.MediaPlayerServiceBinder?> by lazy {
        MutableStateFlow(null)
    }

    override fun getCurrentState(): MediaPlayerServiceState {
        return binder.value?.getCurrentState() ?: if (userActionLock.isLocked) {
            // b/c this is only called when adding a new listener
            // and is synchronized, if the lock is currently
            // held by a user action being processed that will
            // not result in starting the service, thereby dispatching
            // an updated state, that user action processing will
            // come _after_ this is called and will be set properly
            // after the user action processing clears.
            MediaPlayerServiceState.ServiceActive.ServiceLoading
        } else {
            MediaPlayerServiceState.ServiceInactive
        }
    }

    @JvmSynthetic
    fun clearBinderReference() {
        binder.value = null
    }

    @JvmSynthetic
    fun dispatchState(mediaPlayerServiceState: MediaPlayerServiceState) {
        listenerHandler.dispatch(mediaPlayerServiceState)
    }

    inner class MediaPlayerServiceConnection: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null) {
                binder.value = service as MediaPlayerService.MediaPlayerServiceBinder

                dispatchState(MediaPlayerServiceState.ServiceActive.ServiceConnected)
            } else {
                clearBinderReference()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            clearBinderReference()
        }
    }

    private val connection: MediaPlayerServiceConnection by lazy {
        MediaPlayerServiceConnection()
    }

    private val userActionLock = Mutex()
    override suspend fun submitAction(userAction: UserAction) {
        binder.value?.processUserAction(userAction) ?: when (userAction) {
            is UserAction.AdjustSpeed -> {
                repositoryMedia.updateChatMetaData(userAction.chatId, userAction.chatMetaData)
            }
            is UserAction.AdjustSatsPerMinute -> {
                repositoryMedia.updateChatMetaData(userAction.chatId, userAction.chatMetaData)
            }
            is UserAction.SendBoost -> {
                repositoryMedia.streamPodcastPayments(
                    userAction.chatId,
                    userAction.metaData,
                    userAction.podcastId,
                    userAction.metaData.itemId.value,
                    userAction.destinations
                )
            }
            is UserAction.ServiceAction.Pause -> {
                listenerHandler.dispatch(getCurrentState())
            }
            is UserAction.ServiceAction.Play -> {
                userActionLock.withLock {
                    binder.value?.processUserAction(userAction) ?: startService(userAction)
                }
            }
            is UserAction.ServiceAction.Seek -> {
                repositoryMedia.updateChatMetaData(userAction.chatId, userAction.chatMetaData)
                listenerHandler.dispatch(getCurrentState())
            }
        }
    }

    private suspend fun startService(play: UserAction.ServiceAction.Play) {
        try {
            withContext(main) {
                app.startService(play.toIntent(app))
                bindService()

                // Hold the lock until the binder callback has been posted to
                // the ServiceConnection
                binder.collect {
                    if (it != null) {
                        throw RuntimeException()
                    }
                }
            }
        } catch (e: RuntimeException) {}
    }

    @JvmSynthetic
    fun bindService() {
        app.bindService(
            Intent(app, SphinxMediaPlayerService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
    }

    @JvmSynthetic
    fun unbindService() {
        clearBinderReference()
        app.unbindService(connection)
    }
}
