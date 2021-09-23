package chat.sphinx.concept_repository_media

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_podcast.PodcastDestination
import kotlinx.coroutines.flow.Flow
import java.io.File

interface RepositoryMedia {
    fun getChatById(chatId: ChatId): Flow<Chat?>
    fun updateChatMetaData(chatId: ChatId, metaData: ChatMetaData)

    fun downloadMediaIfApplicable(messageId: MessageId)

    fun streamPodcastPayments(
        chatId: ChatId,
        metaData: ChatMetaData,
        podcastId: Long,
        episodeId: Long,
        destinations: List<PodcastDestination>
    )
}
