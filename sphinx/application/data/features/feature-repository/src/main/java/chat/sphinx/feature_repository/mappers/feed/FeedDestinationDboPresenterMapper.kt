package chat.sphinx.feature_repository.mappers.feed

import chat.sphinx.conceptcoredb.FeedDestinationDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_feed.FeedDestination
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class FeedDestinationDboPresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<FeedDestinationDbo, FeedDestination>(dispatchers) {
    override suspend fun mapFrom(value: FeedDestinationDbo): FeedDestination {
        return FeedDestination(
            value.address,
            value.split,
            value.type,
            value.feed_id
        )
    }

    override suspend fun mapTo(value: FeedDestination): FeedDestinationDbo {
        return FeedDestinationDbo(
            value.address,
            value.split,
            value.type,
            value.feedId
        )
    }
}