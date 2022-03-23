package chat.sphinx.concept_network_query_save_profile

import chat.sphinx.concept_network_query_save_profile.model.DeletePeopleProfileDto
import chat.sphinx.concept_network_query_save_profile.model.PeopleProfileDto
import chat.sphinx.concept_network_query_save_profile.model.GetExternalRequestDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

abstract class NetworkQuerySaveProfile {

    abstract fun getExternalRequestByKey(
        host: String,
        key: String
    ): Flow<LoadResponse<GetExternalRequestDto, ResponseError>>

    abstract fun savePeopleProfile(
        profile: PeopleProfileDto,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>

    abstract fun deletePeopleProfile(
        deletePeopleProfileDto: DeletePeopleProfileDto,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null
    ): Flow<LoadResponse<Any, ResponseError>>
}
