package chat.sphinx.feature_service_media_player_android.service

import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class SphinxMediaPlayerService: MediaPlayerService() {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var mediaServiceControllerImplInject: MediaPlayerServiceControllerImpl

    override val mediaServiceController: MediaPlayerServiceControllerImpl
        get() = mediaServiceControllerImplInject
}
