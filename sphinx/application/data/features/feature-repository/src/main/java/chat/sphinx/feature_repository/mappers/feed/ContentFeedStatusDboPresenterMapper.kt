package chat.sphinx.feature_repository.mappers.feed

import chat.sphinx.conceptcoredb.ContentFeedStatusDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_feed.ContentFeedStatus
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class ContentFeedStatusDboPresenterMapper(
    dispatchers: CoroutineDispatchers,
): ClassMapper<ContentFeedStatusDbo, ContentFeedStatus>(dispatchers) {
    override suspend fun mapFrom(value: ContentFeedStatusDbo): ContentFeedStatus {
        return ContentFeedStatus(
            value.feed_id,
            value.feed_url,
            value.subscription_status,
            value.chat_id,
            value.item_id,
            value.sats_per_minute,
            value.player_speed
        )
    }

    override suspend fun mapTo(value: ContentFeedStatus): ContentFeedStatusDbo {
        return ContentFeedStatusDbo(
            value.feedId,
            value.feedUrl,
            value.subscriptionStatus,
            value.chatId,
            value.itemId,
            value.satsPerMinute,
            value.playerSpeed
        )
    }
}