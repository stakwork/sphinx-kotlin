package chat.sphinx.feature_service_media_player_android.service

import android.content.Context
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import chat.sphinx.feature_service_media_player_android.service.components.AudioManagerHandler
import chat.sphinx.feature_service_media_player_android.service.components.SphinxAudioManagerHandler
import chat.sphinx.feature_sphinx_service.ApplicationServiceTracker
import chat.sphinx.logger.SphinxLogger
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_foreground_state.ForegroundStateManager
import javax.inject.Inject

@AndroidEntryPoint
internal class SphinxMediaPlayerService: MediaPlayerService() {

    override val serviceContext: Context
        get() = this

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _applicationServiceTracker: ApplicationServiceTracker

    override val applicationServiceTracker: ApplicationServiceTracker
        get() = _applicationServiceTracker

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _dispatchers: CoroutineDispatchers

    override val dispatchers: CoroutineDispatchers
        get() = _dispatchers

    override val audioManagerHandler: AudioManagerHandler by lazy {
        SphinxAudioManagerHandler(this)
    }

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _foregroundStateManager: ForegroundStateManager

    override val foregroundStateManager: ForegroundStateManager
        get() = _foregroundStateManager

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _LOG: SphinxLogger

    override val LOG: SphinxLogger
        get() = _LOG

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _mediaServiceControllerImpl: MediaPlayerServiceControllerImpl

    override val mediaServiceController: MediaPlayerServiceControllerImpl
        get() = _mediaServiceControllerImpl

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _feedRepository: FeedRepository

    override val feedRepository: FeedRepository
        get() = _feedRepository

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _actionsRepository: ActionsRepository

    override val actionsRepository: ActionsRepository
        get() = _actionsRepository
}
