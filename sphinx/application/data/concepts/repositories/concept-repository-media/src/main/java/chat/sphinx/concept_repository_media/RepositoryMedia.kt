package chat.sphinx.concept_repository_media

import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_feed.DownloadableFeedItem
import chat.sphinx.wrapper_feed.FeedDestination
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_message.Message
import java.io.File

interface RepositoryMedia {

//    fun updateChatMetaData(
//        chatId: ChatId,
//        podcastId: FeedId?,
//        metaData: ChatMetaData,
//        shouldSync: Boolean = true
//    )

    fun updateContentFeedStatus(
        feedId: FeedId,
        feedUrl: FeedUrl,
        subscriptionStatus: Subscribed,
        chatId: ChatId?,
        itemId: FeedId,
        satsPerMinute: Sat,
        playerSpeed: FeedItemDuration
    )

    fun updateContentEpisodeStatus(
        feedId: FeedId,
        itemId: FeedId,
        duration: FeedItemDuration,
        currentTime: FeedItemDuration
    )

    suspend fun updateChatContentSeenAt(chatId: ChatId)

    fun downloadMediaIfApplicable(
        message: Message,
        sent: Boolean,
    )

    fun downloadMediaIfApplicable(
        feedItem: DownloadableFeedItem,
        downloadCompleteCallback: (downloadedFile: File) -> Unit
    )

    fun inProgressDownloadIds(): List<FeedId>

    suspend fun deleteDownloadedMediaIfApplicable(
        feedItem: DownloadableFeedItem
    ): Boolean

    fun streamFeedPayments(
        chatId: ChatId,
//        metaData: ChatMetaData,
        podcastId: String,
        episodeId: String,
        destinations: List<FeedDestination>,
        updateMetaData: Boolean = true,
        clipUUID: MessageUUID? = null
    )
}
