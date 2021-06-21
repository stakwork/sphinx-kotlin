package chat.sphinx.feature_meme_server

import chat.sphinx.concept_meme_server.MemeServerTokenHandler
import chat.sphinx.wrapper_attachment.AuthenticationToken
import chat.sphinx.wrapper_message_media.token.MediaHost

class MemeServerTokenHandlerImpl: MemeServerTokenHandler() {
    override suspend fun retrieveAuthenticationToken(mediaHost: MediaHost): AuthenticationToken? {
        return null
    }
}
