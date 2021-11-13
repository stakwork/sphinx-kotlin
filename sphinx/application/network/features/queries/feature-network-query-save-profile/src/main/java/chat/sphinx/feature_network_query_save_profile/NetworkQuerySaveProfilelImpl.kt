package chat.sphinx.feature_network_query_save_profile

import chat.sphinx.concept_network_query_save_profile.NetworkQuerySaveProfile
import chat.sphinx.concept_network_query_save_profile.model.PersonInfoDto
import chat.sphinx.concept_network_query_save_profile.model.SignBase64Dto
import chat.sphinx.concept_network_query_save_profile.model.SaveProfileDto
import chat.sphinx.concept_network_query_save_profile.model.SaveProfileInfoDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_save_profile.model.SignBase64RelayResponse
import chat.sphinx.feature_network_query_save_profile.model.SaveProfileRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQuerySaveProfileImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQuerySaveProfile() {

    companion object {
        private const val ENDPOINT_SAVE_KEY = "/save/%s"
        private const val ENDPOINT_SAVE_PROFILE = "/profile"
    }

    override fun getProfileByKey(
        host: String,
        key: String
    ): Flow<LoadResponse<PersonInfoDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(
                ENDPOINT_SAVE_KEY,
                host,
                key
            ),
            responseJsonClass = PersonInfoDto::class.java,
        )


    override fun saveProfile(
        data: String
    ): Flow<LoadResponse<PersonInfoDto, ResponseError>> =
        networkRelayCall.relayPost(
            relayEndpoint = ENDPOINT_SAVE_PROFILE,
            requestBody = data,
            requestBodyJsonClass = String::class.java,
            responseJsonClass = SaveProfileRelayResponse::class.java
        )

}
