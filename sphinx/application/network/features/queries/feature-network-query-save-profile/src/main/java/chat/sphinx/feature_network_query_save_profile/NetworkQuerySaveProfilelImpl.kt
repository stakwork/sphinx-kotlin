package chat.sphinx.feature_network_query_save_profile

import chat.sphinx.concept_network_query_save_profile.NetworkQuerySaveProfile
import chat.sphinx.concept_network_query_save_profile.model.PeopleProfileDto
import chat.sphinx.concept_network_query_save_profile.model.GetPeopleProfileDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_save_profile.model.SaveProfileResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQuerySaveProfileImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQuerySaveProfile() {

    companion object {
        private const val ENDPOINT_SAVE_KEY = "https://%s/save/%s"
        private const val ENDPOINT_PROFILE = "/profile"
    }

    override fun getPeopleProfileByKey(
        host: String,
        key: String
    ): Flow<LoadResponse<GetPeopleProfileDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(
                ENDPOINT_SAVE_KEY,
                host,
                key
            ),
            responseJsonClass = GetPeopleProfileDto::class.java,
        )


    override fun savePeopleProfile(
        profile: PeopleProfileDto,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.relayPost(
            relayEndpoint = ENDPOINT_PROFILE,
            requestBody = profile,
            requestBodyJsonClass = PeopleProfileDto::class.java,
            responseJsonClass = SaveProfileResponse::class.java,
            relayData = relayData
        )

    override fun deletePeopleProfile(): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.relayDelete(
            relayEndpoint = ENDPOINT_PROFILE,
            requestBodyJsonClass = null,
            requestBody = null,
            responseJsonClass = SaveProfileResponse::class.java
        )
}
