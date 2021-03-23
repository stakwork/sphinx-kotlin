package chat.sphinx.concept_relay

import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl

@Suppress("NOTHING_TO_INLINE")
suspend inline fun RelayDataHandler.retrieveRelayUrlAndJavaWebToken(): KotlinResponse<
        Pair<JavaWebToken, RelayUrl>,
        ResponseError
        > =

    retrieveRelayUrl()?.let { relayUrl ->
        retrieveJavaWebToken()?.let { jwt ->
            KotlinResponse.Success(Pair(jwt, relayUrl))
        } ?: KotlinResponse.Error(
                ResponseError("Was unable to retrieve the JavaWebToken from storage")
        )
    } ?: KotlinResponse.Error(
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
    abstract suspend fun persistJavaWebToken(token: JavaWebToken?): Boolean
    abstract suspend fun retrieveJavaWebToken(): JavaWebToken?
}
