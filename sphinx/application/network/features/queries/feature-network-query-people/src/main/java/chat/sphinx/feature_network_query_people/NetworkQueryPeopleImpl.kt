package chat.sphinx.feature_network_query_people

import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_network_query_people.model.DeletePeopleProfileDto
import chat.sphinx.concept_network_query_people.model.PeopleProfileDto
import chat.sphinx.concept_network_query_people.model.GetExternalRequestDto
import chat.sphinx.concept_network_query_people.model.TribeMemberProfileDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_people.model.SaveProfileResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_message.MessagePerson
import chat.sphinx.wrapper_message.host
import chat.sphinx.wrapper_message.uuid
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

class NetworkQueryPeopleImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryPeople() {

    companion object {
        private const val ENDPOINT_SAVE_KEY = "https://%s/save/%s"
        private const val ENDPOINT_PROFILE = "/profile"
        private const val ENDPOINT_TRIBE_MEMBER_PROFILE = "https://%s/person/uuid/%s"
    }

    override fun getTribeMemberProfile(
        person: MessagePerson
    ): Flow<LoadResponse<TribeMemberProfileDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(
                ENDPOINT_TRIBE_MEMBER_PROFILE,
                person.host(),
                person.uuid()
            ),
            responseJsonClass = TribeMemberProfileDto::class.java,
        )

    override fun getExternalRequestByKey(
        host: String,
        key: String
    ): Flow<LoadResponse<GetExternalRequestDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(
                ENDPOINT_SAVE_KEY,
                host,
                key
            ),
            responseJsonClass = GetExternalRequestDto::class.java,
        )


    override fun savePeopleProfile(
        profile: PeopleProfileDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.relayPost(
            relayEndpoint = ENDPOINT_PROFILE,
            requestBody = profile,
            requestBodyJsonClass = PeopleProfileDto::class.java,
            responseJsonClass = SaveProfileResponse::class.java,
            relayData = relayData
        )

    override fun deletePeopleProfile(
        deletePeopleProfileDto: DeletePeopleProfileDto,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.relayDelete(
            relayEndpoint = ENDPOINT_PROFILE,
            requestBody = deletePeopleProfileDto,
            requestBodyJsonClass = DeletePeopleProfileDto::class.java,
            responseJsonClass = SaveProfileResponse::class.java,
            relayData = relayData,
            additionalHeaders = mapOf("Content-Type" to "application/json;charset=utf-8")
        )
}
