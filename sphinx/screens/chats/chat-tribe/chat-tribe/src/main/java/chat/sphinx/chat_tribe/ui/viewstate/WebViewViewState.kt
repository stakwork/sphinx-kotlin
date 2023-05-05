package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.concept_network_query_people.model.BadgeDto
import chat.sphinx.concept_network_query_people.model.ChatLeaderboardDto
import chat.sphinx.concept_network_query_people.model.TribeMemberProfileDto
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_message.MessagePerson
import chat.sphinx.wrapper_message.SenderAlias
import io.matthewnelson.concept_views.viewstate.ViewState


sealed class WebViewViewState: ViewState<WebViewViewState>() {

    object Idle: WebViewViewState()
    object Authorization: WebViewViewState()

}