package chat.sphinx.feature_network_query_attachment

import chat.sphinx.concept_network_query_attachment.NetworkQueryAttachment
import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationTokenDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentChallengeSigDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_attachment.model.SignChallengeRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_attachment.AuthenticationChallenge
import chat.sphinx.wrapper_attachment.AuthenticationId
import chat.sphinx.wrapper_attachment.AuthenticationSig
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_message_media.token.MediaHost
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import kotlinx.coroutines.flow.Flow

class NetworkQueryAttachmentImpl(
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryAttachment() {

    companion object {
        private const val ATTACHMENTS_SERVER_URL = "https://%s"

        private const val ENDPOINT_ASK_AUTHENTICATION = "$ATTACHMENTS_SERVER_URL/ask"
        private const val ENDPOINT_SIGNER = "/signer/%s"
        private const val ENDPOINT_VERIFY_AUTHENTICATION = "$ATTACHMENTS_SERVER_URL/verify?id=%s&sig=%s&pubkey=%s"
    }

    override fun askAuthentication(
        memeServerHost: MediaHost
    ): Flow<LoadResponse<AttachmentAuthenticationDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(ENDPOINT_ASK_AUTHENTICATION, memeServerHost.value),
            responseJsonClass = AttachmentAuthenticationDto::class.java,
        )

    override fun signChallenge(
        challenge: AuthenticationChallenge,
        relayData: Pair<AuthorizationToken, RelayUrl>?
    ): Flow<LoadResponse<AttachmentChallengeSigDto, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = SignChallengeRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_SIGNER, challenge.value),
            relayData = relayData,
        )

    override fun verifyAuthentication(
        id: AuthenticationId,
        sig: AuthenticationSig,
        ownerPubKey: LightningNodePubKey,
        memeServerHost: MediaHost,
    ): Flow<LoadResponse<AttachmentAuthenticationTokenDto, ResponseError>> =
        networkRelayCall.post(
            url = String.format(
                ENDPOINT_VERIFY_AUTHENTICATION,
                memeServerHost.value,
                id.value,
                sig.value,
                ownerPubKey.value
            ),
            responseJsonClass = AttachmentAuthenticationTokenDto::class.java,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
        )
}
