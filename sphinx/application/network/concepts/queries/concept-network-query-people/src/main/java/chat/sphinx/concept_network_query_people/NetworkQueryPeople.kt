package chat.sphinx.concept_network_query_people

import chat.sphinx.concept_network_query_people.model.DeletePeopleProfileDto
import chat.sphinx.concept_network_query_people.model.PeopleProfileDto
import chat.sphinx.concept_network_query_people.model.GetExternalRequestDto
import chat.sphinx.concept_network_query_people.model.TribeMemberProfileDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
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

    abstract fun savePeopleProfile(
        profile: PeopleProfileDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>

    abstract fun deletePeopleProfile(
        deletePeopleProfileDto: DeletePeopleProfileDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>

    abstract fun getTribeMemberProfile(
        person: MessagePerson
    ): Flow<LoadResponse<TribeMemberProfileDto, ResponseError>>
}
