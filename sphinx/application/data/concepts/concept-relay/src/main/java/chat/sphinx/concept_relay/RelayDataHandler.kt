package chat.sphinx.concept_relay

import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import chat.sphinx.wrapper_rsa.RsaPublicKey

@Suppress("NOTHING_TO_INLINE")
suspend inline fun RelayDataHandler.retrieveRelayUrlAndToken(): Response<
        Triple<AuthorizationToken, TransportToken?, RelayUrl>,
        ResponseError
        > {

    return retrieveRelayUrl()?.let { relayUrl ->
        retrieveAuthorizationToken()?.let { jwt ->
            retrieveRelayTransportToken(jwt)?.let { tt ->
                Response.Success(Triple(jwt, tt, relayUrl))
            } ?: Response.Success(Triple(jwt, null, relayUrl))
        } ?: Response.Error(
            ResponseError("Was unable to retrieve the AuthorizationToken from storage")
        )
    } ?: Response.Error(
        ResponseError("Was unable to retrieve the RelayURL from storage")
    )
}

/**
 * Persists and retrieves Sphinx Relay data to device storage. Implementation
 * requires User to be logged in to work, otherwise `null` and `false` are always
 * returned.
 * */
abstract class RelayDataHandler {
    /**
     * Upon persisting of the [RelayUrl], depending on if it is an onion address, it will
     * set tor network requirement to true or false.
     * */
    abstract suspend fun persistRelayUrl(url: RelayUrl): Boolean
    abstract suspend fun retrieveRelayUrl(): RelayUrl?

    /**
     * Send `null` to clear the token from persistent storage
     * */
    abstract suspend fun persistAuthorizationToken(token: AuthorizationToken?): Boolean
    abstract suspend fun retrieveAuthorizationToken(): AuthorizationToken?

    /**
     * Send `null` to clear the relay transport key from persistent storage
     * */
    abstract suspend fun persistRelayTransportKey(key: RsaPublicKey?): Boolean
    abstract suspend fun retrieveRelayTransportKey(): RsaPublicKey?

    abstract suspend fun retrieveRelayTransportToken(
        authorizationToken: AuthorizationToken
    ): TransportToken?

    /**
     * Will parse the [relayUrl] for a proper scheme (http or https). If a scheme is
     * not present, will determine whether or not http or https will be used depending
     * on if the [RelayUrl] is an onion address or not.
     *
     *  - If it contains a scheme (http or https), returns [relayUrl] value passed, unmodified
     *  - If it *is not* an onion address, defaults to `https`
     *  - If it *is* an onion address, defaults to `http`
     *
     * Returns a properly formatted [RelayUrl]
     * */
    abstract fun formatRelayUrl(relayUrl: RelayUrl): RelayUrl
}
