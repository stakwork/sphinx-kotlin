package chat.sphinx.concept_network_query_chat

import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

interface NetworkQueryChat {
    fun getChats(): Flow<KotlinResponse<List<ChatDto>>>
    fun getChats(javaWebToken: JavaWebToken, relayUrl: RelayUrl): Flow<KotlinResponse<List<ChatDto>>>
}
