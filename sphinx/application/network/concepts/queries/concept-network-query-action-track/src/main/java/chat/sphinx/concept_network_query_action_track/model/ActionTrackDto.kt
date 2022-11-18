package chat.sphinx.concept_network_query_action_track.model

import chat.sphinx.wrapper_action_track.action_wrappers.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class ActionTrackDto(
    val type: Int,
    val meta_data: ActionTrackMetaDataDto
)

@JsonClass(generateAdapter = true)
data class ActionTrackMetaDataDto(
    //Message Actions
    val keywords: List<String>? = null,

    //Feed Search
    val search_term: String? = null,
    val frequency: Long? = null,

    //Content Boost
    val boost: Long? = null,
    val feed_id: String? = null,
    val feed_type: Long? = null,
    val feed_url: String? = null,
    val feed_item_id: String? = null,
    val feed_item_url: String? = null,
    val topics: List<String>? = null,

    //Podcast Clip
    val start_timestamp: Long? = null,
    val endTimestamp: Long? = null,

    //Content Consumed
    val history: List<ActionTrackHistoryItemDto>? = null,

    //General
    val current_timestamp: Long? = null,
)

@JsonClass(generateAdapter = true)
data class ActionTrackHistoryItemDto(
    val topics: List<String>,
    val start_timestamp: Long,
    val end_timestamp: Long,
    val current_timestamp: Long
)

@Suppress("NOTHING_TO_INLINE")
inline fun String.toActionTrackMetaDataDtoOrNull(
    moshi: Moshi
): ActionTrackMetaDataDto? {
    return this.toMessageActionOrNull(moshi)?.let { messageAction ->
        ActionTrackMetaDataDto(
            keywords = messageAction.keywords,
            current_timestamp = messageAction.currentTimestamp
        )
    } ?: this.toFeedSearchActionOrNull(moshi)?.let { feedSearchAction ->
        ActionTrackMetaDataDto(
            search_term = feedSearchAction.searchTerm,
            frequency = feedSearchAction.frequency,
            current_timestamp = feedSearchAction.currentTimestamp
        )
    } ?: this.toPodcastClipCommentActionOrNull(moshi)?.let { podcastClipAction ->
        ActionTrackMetaDataDto(
            feed_id = podcastClipAction.feedId,
            feed_type = podcastClipAction.feedType,
            feed_url = podcastClipAction.feedUrl,
            feed_item_id = podcastClipAction.feedItemId,
            feed_item_url = podcastClipAction.feedItemUrl,
            topics = podcastClipAction.topics,
            start_timestamp = podcastClipAction.startTimestamp,
            endTimestamp = podcastClipAction.endTimestamp,
            current_timestamp = podcastClipAction.currentTimestamp
        )
    } ?: this.toContentBoostActionOrNull(moshi)?.let { contentBoost ->
        ActionTrackMetaDataDto(
            boost = contentBoost.boost,
            feed_id = contentBoost.feedId,
            feed_type = contentBoost.feedType,
            feed_url = contentBoost.feedUrl,
            feed_item_id = contentBoost.feedItemId,
            feed_item_url = contentBoost.feedItemUrl,
            topics = contentBoost.topics,
            current_timestamp = contentBoost.currentTimestamp
        )
    } ?: this.toContentConsumedAction(moshi)?.let { contentConsumed ->

        val items: MutableList<ActionTrackHistoryItemDto> = mutableListOf()

        contentConsumed.history.forEach { historyItem ->
            items.add(
                ActionTrackHistoryItemDto(
                    topics = historyItem.topics,
                    start_timestamp = historyItem.startTimestamp,
                    end_timestamp = historyItem.endTimestamp,
                    current_timestamp = historyItem.currentTimestamp
                )
            )
        }

        ActionTrackMetaDataDto(
            feed_id = contentConsumed.feedId,
            feed_type = contentConsumed.feedType,
            feed_url = contentConsumed.feedUrl,
            feed_item_id = contentConsumed.feedItemId,
            feed_item_url = contentConsumed.feedItemUrl,
            history = items
        )
    }
}

