package chat.sphinx.concept_repository_media

import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.DownloadableFeedItem
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_podcast.PodcastDestination
import java.io.File

interface RepositoryMedia {

    fun updateChatMetaData(chatId: ChatId, metaData: ChatMetaData, shouldSync: Boolean = true)

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

    fun streamPodcastPayments(
        chatId: ChatId,
        metaData: ChatMetaData,
        podcastId: String,
        episodeId: String,
        destinations: List<PodcastDestination>
    )
}
