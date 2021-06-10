package chat.sphinx.feature_service_media_player_android.service

import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@AndroidEntryPoint
internal class SphinxMediaPlayerService: MediaPlayerService() {

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _mediaServiceControllerImpl: MediaPlayerServiceControllerImpl

    override val mediaServiceController: MediaPlayerServiceControllerImpl
        get() = _mediaServiceControllerImpl

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _dispatchers: CoroutineDispatchers

    override val dispatchers: CoroutineDispatchers
        get() = _dispatchers

    @Inject
    @Suppress("PropertyName", "ProtectedInFinal")
    protected lateinit var _repositoryMedia: RepositoryMedia

    override val repositoryMedia: RepositoryMedia
        get() = _repositoryMedia
}
