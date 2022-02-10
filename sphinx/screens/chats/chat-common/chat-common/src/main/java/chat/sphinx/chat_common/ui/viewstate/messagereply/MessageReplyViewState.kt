package chat.sphinx.chat_common.ui.viewstate.messagereply

import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.PodcastClip
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class MessageReplyViewState: ViewState<MessageReplyViewState>() {

    object ReplyingDismissed: MessageReplyViewState()

    class ReplyingToMessage(
        val message: Message,
        val senderAlias: String,
    ): MessageReplyViewState()

    class CommentingOnPodcast(
        val podcastClip: PodcastClip,
    ): MessageReplyViewState()
}