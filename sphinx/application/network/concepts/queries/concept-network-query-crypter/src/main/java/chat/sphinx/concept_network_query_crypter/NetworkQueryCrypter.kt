package chat.sphinx.concept_network_query_crypter

import chat.sphinx.concept_network_query_crypter.model.CrypterPublicKeyResultDto
import chat.sphinx.concept_network_query_crypter.model.SendSeedDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryCrypter {

    ///////////
    /// GET ///
    ///////////
    abstract fun getCrypterPubKey(): Flow<LoadResponse<CrypterPublicKeyResultDto, ResponseError>>

    abstract fun sendEncryptedSeed(
        seedDto: SendSeedDto,
    ): Flow<LoadResponse<Any, ResponseError>>
}