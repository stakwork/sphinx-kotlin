package chat.sphinx.podcast_player_view_model_coordinator.request

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl

class PodcastPlayerRequest(
    val chatId: ChatId,
    val feedId: FeedId,
    val feedUrl: FeedUrl
)