package chat.sphinx.concept_network_query_people

import chat.sphinx.concept_network_query_people.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_message.MessagePerson
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryPeople {

    abstract fun getExternalRequestByKey(
        host: String,
        key: String
    ): Flow<LoadResponse<GetExternalRequestDto, ResponseError>>

    abstract fun getTribeMemberProfile(
        person: MessagePerson
    ): Flow<LoadResponse<TribeMemberProfileDto, ResponseError>>

    abstract fun getLeaderboard(
        tribeUUID: ChatUUID,
    ): Flow<LoadResponse<List<ChatLeaderboardDto>, ResponseError>>

    abstract fun getKnownBadges(
        badgeIds: Array<Long>,
    ): Flow<LoadResponse<List<BadgeDto>, ResponseError>>

    abstract fun getBadgesByPerson(
        person: MessagePerson,
    ): Flow<LoadResponse<List<BadgeDto>, ResponseError>>

}
