package chat.sphinx.concept_meme_server

import chat.sphinx.wrapper_attachment.AuthenticationToken
import chat.sphinx.wrapper_message_media.token.MediaHost

abstract class MemeServerTokenHandler {
    abstract suspend fun retrieveAuthenticationToken(
        mediaHost: MediaHost
    ): AuthenticationToken?
}
