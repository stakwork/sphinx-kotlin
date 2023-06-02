package chat.sphinx.concept_repository_media

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.DownloadableFeedItem
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message_media.MessageMedia
import kotlinx.coroutines.flow.Flow
import java.io.File

interface RepositoryMedia {

    fun downloadMediaIfApplicable(
        message: Message,
        sent: Boolean,
    )

    fun downloadMediaIfApplicable(
        feedItem: DownloadableFeedItem,
        downloadCompleteCallback: (downloadedFile: File) -> Unit
    )

    fun getAllMessageMediaByChatId(chatId: ChatId): Flow<List<MessageMedia>>

    fun getAllDownloadedMedia(): Flow<List<MessageMedia>>
    fun getAllDownloadedMediaByChatId(chatId: ChatId): Flow<List<MessageMedia>>
    fun deleteDownloadedMediaByChatId(chatId: ChatId, files: List<File>)

    fun inProgressDownloadIds(): List<FeedId>

    suspend fun deleteDownloadedMediaIfApplicable(
        feedItem: DownloadableFeedItem
    ): Boolean

    suspend fun deleteAllFeedDownloadedMedia(
        feed: Feed
    ): Boolean


}
