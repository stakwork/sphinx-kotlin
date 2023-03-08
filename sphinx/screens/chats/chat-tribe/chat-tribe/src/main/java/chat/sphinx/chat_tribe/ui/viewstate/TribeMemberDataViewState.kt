package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.concept_network_query_people.model.BadgeDto
import chat.sphinx.concept_network_query_people.model.ChatLeaderboardDto
import chat.sphinx.concept_network_query_people.model.TribeMemberProfileDto
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_message.MessagePerson
import chat.sphinx.wrapper_message.SenderAlias
import io.matthewnelson.concept_views.viewstate.ViewState


sealed class TribeMemberDataViewState: ViewState<TribeMemberDataViewState>() {

    object Idle: TribeMemberDataViewState()

    class TribeMemberPopup(
        val messageUUID: MessageUUID,
        val memberName: SenderAlias,
        val colorKey: String,
        val memberPic: PhotoUrl?
    ): TribeMemberDataViewState()

    object LoadingTribeMemberProfile: TribeMemberDataViewState()

    class TribeMemberProfile(
        val messageUUID: MessageUUID?,
        val profile: TribeMemberProfileDto,
        val leaderboard: ChatLeaderboardDto?,
        val badges: List<BadgeDto>?,
    ): TribeMemberDataViewState()

}