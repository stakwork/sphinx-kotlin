package chat.sphinx.concept_network_query_verify_external

import chat.sphinx.concept_network_query_verify_external.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryAuthorizeExternal {

    abstract fun authorizeExternal(
        host: String,
        challenge: String,
        token: String,
        info: VerifyExternalInfoDto,
    ): Flow<LoadResponse<Any, ResponseError>>

    abstract fun redeemSats(
        host: String,
        info: RedeemSatsDto,
    ): Flow<LoadResponse<Any, ResponseError>>

    abstract fun getPersonInfo(
        host: String,
        publicKey: String
    ): Flow<LoadResponse<PersonInfoDto, ResponseError>>
}