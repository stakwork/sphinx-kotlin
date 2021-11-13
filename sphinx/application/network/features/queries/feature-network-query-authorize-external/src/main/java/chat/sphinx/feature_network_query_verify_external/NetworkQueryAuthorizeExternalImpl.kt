package chat.sphinx.feature_network_query_verify_external

import chat.sphinx.concept_network_query_verify_external.NetworkQueryAuthorizeExternal
import chat.sphinx.concept_network_query_verify_external.model.PersonInfoDto
import chat.sphinx.concept_network_query_verify_external.model.SignBase64Dto
import chat.sphinx.concept_network_query_verify_external.model.VerifyExternalDto
import chat.sphinx.concept_network_query_verify_external.model.VerifyExternalInfoDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_verify_external.model.SignBase64RelayResponse
import chat.sphinx.feature_network_query_verify_external.model.VerifyExternalRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQueryAuthorizeExternalImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryAuthorizeExternal() {

    companion object {
        private const val ENDPOINT_VERIFY_EXTERNAL = "/verify_external"
        private const val ENDPOINT_SIGN_BASE_64 = "/signer/%s"
        private const val ENDPOINT_SAVE_KEY = "/save/%s"
        private const val ENDPOINT_AUTHORIZE_EXTERNAL = "https://%s/verify/%s?token=%s"
        private const val ENDPOINT_GET_PERSON_INFO = "https://%s/person/%s"
    }

    override fun verifyExternal(
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<VerifyExternalDto, ResponseError>> =
        networkRelayCall.relayPost(
            responseJsonClass = VerifyExternalRelayResponse::class.java,
            relayEndpoint = ENDPOINT_VERIFY_EXTERNAL,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
            relayData = relayData
        )

    override fun signBase64(
        base64: String,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<SignBase64Dto, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = SignBase64RelayResponse::class.java,
            relayEndpoint = "${String.format(ENDPOINT_SIGN_BASE_64, base64)}",
            relayData = relayData
        )

//
//    override fun createPeopleProfile(
//        relayData: Pair<AuthorizationToken, RelayUrl>?
//    ): Flow<LoadResponse<VerifyExternalDto, ResponseError>> =
//        networkRelayCall.relayPost(
//            responseJsonClass = VerifyExternalRelayResponse::class.java,
//            relayEndpoint = ENDPOINT_CREATE_PEOPLE_PROFILE,
//            requestBodyJsonClass = Map::class.java,
//            requestBody = mapOf(Pair("", "")),
//            relayData = relayData
//        )

    override fun authorizeExternal(
        host: String,
        challenge: String,
        token: String,
        info: VerifyExternalInfoDto,
    ): Flow<LoadResponse<Any, ResponseError>> =
//        networkRelayCall.get("https://9589-2600-1700-5520-a400-b444-b6b0-7bb7-53ba.ngrok.io",
//            responseJsonClass = Any::class.java,
//            )
        networkRelayCall.post(
            url = String.format(
                ENDPOINT_AUTHORIZE_EXTERNAL,
                host,
                challenge,
                token
            ),
            responseJsonClass = Any::class.java,
            requestBodyJsonClass = VerifyExternalInfoDto::class.java,
            requestBody = info,
        )

    override fun getPersonInfo(
        host: String,
        key: String
    ): Flow<LoadResponse<PersonInfoDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(
                ENDPOINT_GET_PERSON_INFO,
                host,
                key
            ),
            responseJsonClass = PersonInfoDto::class.java,
        )

    override fun saveProfile(
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


}
