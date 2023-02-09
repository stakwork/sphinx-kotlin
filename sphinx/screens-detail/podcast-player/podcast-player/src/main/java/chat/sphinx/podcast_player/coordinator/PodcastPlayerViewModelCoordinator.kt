package chat.sphinx.podcast_player.coordinator

import chat.sphinx.concept_view_model_coordinator.RequestHolder
import chat.sphinx.feature_view_model_coordinator.ViewModelCoordinatorImpl
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.podcast_player.navigation.BackType
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.podcast_player_view_model_coordinator.request.PodcastPlayerRequest
import chat.sphinx.podcast_player_view_model_coordinator.response.PodcastPlayerResponse
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class PodcastPlayerViewModelCoordinator(
    dispatchers: CoroutineDispatchers,
    private val podcastPlayerNavigator: PodcastPlayerNavigator,
    LOG: SphinxLogger,
): ViewModelCoordinatorImpl<BackType, PodcastPlayerRequest, PodcastPlayerResponse>(
    LOG = LOG,
    dispatcher = dispatchers.mainImmediate
) {
    override suspend fun navigateBack(back: BackType) {
        when (back) {
            is BackType.CloseDetailScreen -> {
                podcastPlayerNavigator.closeDetailScreen()
            }
            is BackType.PopBackStack -> {
                podcastPlayerNavigator.popBackStack()
            }
        }
    }

    override suspend fun navigateToScreen(request: RequestHolder<PodcastPlayerRequest>) {
        podcastPlayerNavigator.toPodcastPlayerScreen(
            request.request.chatId,
            request.request.feedId,
            request.request.feedUrl
        )
    }

    override suspend fun checkRequest(request: PodcastPlayerRequest): PodcastPlayerResponse? {
        return null
    }
}