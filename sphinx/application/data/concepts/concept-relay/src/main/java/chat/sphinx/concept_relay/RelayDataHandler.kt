package chat.sphinx.concept_relay

import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl

@Suppress("NOTHING_TO_INLINE")
suspend inline fun RelayDataHandler.retrieveRelayUrlAndAuthorizationToken(): Response<
        Pair<AuthorizationToken, RelayUrl>,
        ResponseError
        > =

    retrieveRelayUrl()?.let { relayUrl ->
        retrieveAuthorizationToken()?.let { jwt ->
            Response.Success(Pair(jwt, relayUrl))
        } ?: Response.Error(
                ResponseError("Was unable to retrieve the AuthorizationToken from storage")
        )
    } ?: Response.Error(
            ResponseError("Was unable to retrieve the RelayURL from storage")
    )

/**
 * Persists and retrieves Sphinx Relay data to device storage. Implementation
 * requires User to be logged in to work, otherwise `null` and `false` are always
 * returned.
 * */
abstract class RelayDataHandler {
    abstract suspend fun persistRelayUrl(url: RelayUrl): Boolean
    abstract suspend fun retrieveRelayUrl(): RelayUrl?

    /**
     * Send `null` to clear the token from persistent storage
     * */
    abstract suspend fun persistAuthorizationToken(token: AuthorizationToken?): Boolean
    abstract suspend fun retrieveAuthorizationToken(): AuthorizationToken?
}
