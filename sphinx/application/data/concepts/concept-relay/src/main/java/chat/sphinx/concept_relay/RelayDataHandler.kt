package chat.sphinx.concept_relay

import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl

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
