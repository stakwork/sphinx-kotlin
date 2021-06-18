package chat.sphinx.concept_network_query_attachment

import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationTokenDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentChallengeSigDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_attachment.AuthenticationChallenge
import chat.sphinx.wrapper_attachment.AuthenticationId
import chat.sphinx.wrapper_attachment.AuthenticationSig
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryAttachment {

    abstract fun askAuthentication(): Flow<LoadResponse<AttachmentAuthenticationDto, ResponseError>>

    abstract fun signChallenge(
        challenge: AuthenticationChallenge,
    ): Flow<LoadResponse<AttachmentChallengeSigDto, ResponseError>>

    abstract fun verifyAuthentication(
        id: AuthenticationId,
        sig: AuthenticationSig,
        pubKey: LightningNodePubKey,
    ): Flow<LoadResponse<AttachmentAuthenticationTokenDto, ResponseError>>
}
