package chat.sphinx.podcast_player.di

import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.podcast_player.coordinator.PodcastPlayerViewModelCoordinator
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.podcast_player_view_model_coordinator.request.PodcastPlayerRequest
import chat.sphinx.podcast_player_view_model_coordinator.response.PodcastPlayerResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object PodcastPlayerModule {

    @Provides
    @ActivityRetainedScoped
    fun provideScannerCoordinator(
        dispatchers: CoroutineDispatchers,
        scannerNavigator: PodcastPlayerNavigator,
        LOG: SphinxLogger,
    ): PodcastPlayerViewModelCoordinator =
        PodcastPlayerViewModelCoordinator(dispatchers, scannerNavigator, LOG)

    @Provides
    fun provideViewModelCoordinator(
        podcastPlayerViewModelCoordinator: PodcastPlayerViewModelCoordinator
    ): ViewModelCoordinator<PodcastPlayerRequest, PodcastPlayerResponse> =
        podcastPlayerViewModelCoordinator
}