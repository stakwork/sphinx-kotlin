package chat.sphinx.concept_meme_server

import chat.sphinx.concept_repository_connect_manager.ConnectManagerRepository
import chat.sphinx.wrapper_meme_server.AuthenticationToken
import chat.sphinx.wrapper_message_media.token.MediaHost

abstract class MemeServerTokenHandler {
    abstract suspend fun retrieveAuthenticationToken(
        mediaHost: MediaHost
    ): AuthenticationToken?

    abstract fun addListener(
        listener: ConnectManagerRepository
    )
}
