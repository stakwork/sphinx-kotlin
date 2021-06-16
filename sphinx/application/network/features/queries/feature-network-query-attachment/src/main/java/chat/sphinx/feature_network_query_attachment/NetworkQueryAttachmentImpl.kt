package chat.sphinx.feature_network_query_attachment

import chat.sphinx.concept_network_query_attachment.NetworkQueryAttachment
import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationTokenDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentChallengeSigDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_attachment.model.SignChallengeRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.attachment_authentication.AuthenticationChallenge
import chat.sphinx.wrapper_common.attachment_authentication.AuthenticationId
import chat.sphinx.wrapper_common.attachment_authentication.AuthenticationSig
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import kotlinx.coroutines.flow.Flow

class NetworkQueryAttachmentImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryAttachment() {

    companion object {
        private const val ATTACHMENTS_SERVER_URL = "https://memes.sphinx.chat"

        private const val ENDPOINT_ASK_AUTHENTICATION = "$ATTACHMENTS_SERVER_URL/ask"
        private const val ENDPOINT_SIGNER = "/signer/%s"
        private const val ENDPOINT_VERIFY_AUTHENTICATION = "$ATTACHMENTS_SERVER_URL/verify?id=%s&sig=%s&pubkey=%s"

    }

    override fun askAuthentication(): Flow<LoadResponse<AttachmentAuthenticationDto, ResponseError>> =
        networkRelayCall.get(
            url = ENDPOINT_ASK_AUTHENTICATION,
            responseJsonClass = AttachmentAuthenticationDto::class.java,
        )

    override fun signChallenge(
        challenge: AuthenticationChallenge,
    ): Flow<LoadResponse<AttachmentChallengeSigDto, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = SignChallengeRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_SIGNER, challenge.value),
            relayData = null
        )

    override fun verifyAuthentication(
        id: AuthenticationId,
        sig: AuthenticationSig,
        pubKey: LightningNodePubKey,
    ): Flow<LoadResponse<AttachmentAuthenticationTokenDto, ResponseError>> =
        networkRelayCall.post(
            url = String.format(ENDPOINT_VERIFY_AUTHENTICATION, id.value, sig.value, pubKey.value),
            responseJsonClass = AttachmentAuthenticationTokenDto::class.java,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
        )

}