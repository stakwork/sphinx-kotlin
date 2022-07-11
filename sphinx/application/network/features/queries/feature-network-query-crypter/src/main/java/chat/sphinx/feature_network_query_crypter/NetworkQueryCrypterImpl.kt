package chat.sphinx.feature_network_query_crypter

import chat.sphinx.concept_network_query_crypter.NetworkQueryCrypter
import chat.sphinx.concept_network_query_crypter.model.CrypterPublicKeyResultDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.flow.Flow

class NetworkQueryCrypterImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryCrypter() {

    companion object {
        private const val DEFAULT_SERVER_URL = "http://192.168.0.23:8000"

        private const val ENDPOINT_GET_PUBLIC_KEY = "$DEFAULT_SERVER_URL/ecdh"
        private const val ENDPOINT_POST_ENCRYPTED_SEED = "$DEFAULT_SERVER_URL/config?config=%s"
    }

    override fun getCrypterPubKey(): Flow<LoadResponse<CrypterPublicKeyResultDto, ResponseError>> =
        networkRelayCall.get(
            url = ENDPOINT_GET_PUBLIC_KEY,
            responseJsonClass = CrypterPublicKeyResultDto::class.java,
        )

    override fun sendEncryptedSeed(
        seed: String,
        publicKey: String,
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.post(
            url = String.format(ENDPOINT_POST_ENCRYPTED_SEED, "{\"seed\":\"$seed\",\"ssid\":\"xxx\",\"pass\":\"xxx\",\"broker\":\"xxx\",\"pubkey\":\"$publicKey\",\"network\":\"regtest\"}"),
            responseJsonClass = Any::class.java,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
        )

}