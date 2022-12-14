package chat.sphinx.chat_common.ui.viewstate.mentions

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class MessageMentionsViewState: ViewState<MessageMentionsViewState>() {

    class MessageMentions(
        val mentions: List<String>,
    ): MessageMentionsViewState()
}