package chat.sphinx.concept_network_query_attachment

import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationTokenDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentChallengeSigDto
import chat.sphinx.concept_network_query_attachment.model.PostAttachmentUploadDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_attachment.AuthenticationChallenge
import chat.sphinx.wrapper_attachment.AuthenticationId
import chat.sphinx.wrapper_attachment.AuthenticationSig
import chat.sphinx.wrapper_attachment.AuthenticationToken
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.token.MediaHost
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import io.matthewnelson.crypto_common.clazzes.Password
import kotlinx.coroutines.flow.Flow
import java.io.File

abstract class NetworkQueryAttachment {

    abstract fun askAuthentication(
        memeServerHost: MediaHost = MediaHost.DEFAULT,
    ): Flow<LoadResponse<AttachmentAuthenticationDto, ResponseError>>

    abstract fun signChallenge(
        challenge: AuthenticationChallenge,
        relayData: Pair<AuthorizationToken, RelayUrl>? = null,
    ): Flow<LoadResponse<AttachmentChallengeSigDto, ResponseError>>

    abstract fun verifyAuthentication(
        id: AuthenticationId,
        sig: AuthenticationSig,
        ownerPubKey: LightningNodePubKey,
        memeServerHost: MediaHost = MediaHost.DEFAULT,
    ): Flow<LoadResponse<AttachmentAuthenticationTokenDto, ResponseError>>

    abstract suspend fun uploadAttachment(
        authenticationToken: AuthenticationToken,
        mediaType: MediaType,
        file: File,
        password: Password?,
        memeServerHost: MediaHost = MediaHost.DEFAULT,
    ): Response<PostAttachmentUploadDto, ResponseError>
}
