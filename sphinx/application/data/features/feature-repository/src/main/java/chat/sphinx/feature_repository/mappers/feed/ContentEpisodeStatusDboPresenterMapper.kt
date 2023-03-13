package chat.sphinx.feature_repository.mappers.feed

import chat.sphinx.conceptcoredb.ContentEpisodeStatusDbo
import chat.sphinx.conceptcoredb.FeedModelDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_feed.ContentEpisodeStatus
import chat.sphinx.wrapper_feed.FeedModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class ContentEpisodeStatusDboPresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<ContentEpisodeStatusDbo, ContentEpisodeStatus>(dispatchers) {
    override suspend fun mapFrom(value: ContentEpisodeStatusDbo): ContentEpisodeStatus {
        return ContentEpisodeStatus(
            value.feed_id,
            value.item_id,
            value.duration,
            value.current_time,
            value.played
        )
    }

    override suspend fun mapTo(value: ContentEpisodeStatus): ContentEpisodeStatusDbo {
        return ContentEpisodeStatusDbo(
            value.feedId,
            value.itemId,
            value.duration,
            value.currentTime,
            value.played
        )
    }
}