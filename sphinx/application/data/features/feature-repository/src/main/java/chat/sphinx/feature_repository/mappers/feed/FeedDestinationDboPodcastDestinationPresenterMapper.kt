package chat.sphinx.feature_repository.mappers.feed

import chat.sphinx.conceptcoredb.FeedDestinationDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_podcast.PodcastDestination
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class FeedDestinationDboPodcastDestinationPresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<FeedDestinationDbo, PodcastDestination>(dispatchers) {
    override suspend fun mapFrom(value: FeedDestinationDbo): PodcastDestination {
        return PodcastDestination(
            split = value.split,
            address = value.address,
            type = value.type,
            podcastId = value.feed_id
        )
    }

    override suspend fun mapTo(value: PodcastDestination): FeedDestinationDbo {
        return FeedDestinationDbo(
            address = value.address,
            split = value.split,
            type = value.type,
            feed_id = value.podcastId
        )
    }
}