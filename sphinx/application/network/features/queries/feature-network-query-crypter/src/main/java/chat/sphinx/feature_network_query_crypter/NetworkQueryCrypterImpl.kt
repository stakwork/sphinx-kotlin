package chat.sphinx.feature_network_query_crypter

import chat.sphinx.concept_network_query_crypter.NetworkQueryCrypter
import chat.sphinx.concept_network_query_crypter.model.CrypterPublicKeyResultDto
import chat.sphinx.concept_network_query_crypter.model.SendSeedDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import kotlinx.coroutines.flow.Flow

class NetworkQueryCrypterImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryCrypter() {

    companion object {
        private const val DEFAULT_SERVER_URL = "http://192.168.71.1"
//        private const val DEFAULT_SERVER_URL = "http://192.168.0.25:8000"

        private const val ENDPOINT_GET_PUBLIC_KEY = "$DEFAULT_SERVER_URL/ecdh"
        private const val ENDPOINT_POST_ENCRYPTED_SEED = "$DEFAULT_SERVER_URL/config?config=%s"
    }

    override fun getCrypterPubKey(): Flow<LoadResponse<CrypterPublicKeyResultDto, ResponseError>> =
        networkRelayCall.get(
            url = ENDPOINT_GET_PUBLIC_KEY,
            responseJsonClass = CrypterPublicKeyResultDto::class.java,
        )

    override fun sendEncryptedSeed(
        seedDto: SendSeedDto,
    ): Flow<LoadResponse<Any, ResponseError>> =
        networkRelayCall.post(
            url = String.format(ENDPOINT_POST_ENCRYPTED_SEED, "{\"seed\":\"${seedDto.seed}\",\"ssid\":\"${seedDto.ssid}\",\"pass\":\"${seedDto.pass}\",\"broker\":\"${seedDto.lightningNodeUrl}\",\"pubkey\":\"${seedDto.pubkey}\",\"network\":\"${seedDto.network}\"}"),
            responseJsonClass = Any::class.java,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
        )

}