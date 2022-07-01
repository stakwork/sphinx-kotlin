package chat.sphinx.concept_network_query_meme_server

import chat.sphinx.concept_network_query_meme_server.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_io_utils.InputStreamProvider
import chat.sphinx.wrapper_meme_server.AuthenticationChallenge
import chat.sphinx.wrapper_meme_server.AuthenticationId
import chat.sphinx.wrapper_meme_server.AuthenticationSig
import chat.sphinx.wrapper_meme_server.AuthenticationToken
import chat.sphinx.wrapper_message_media.FileName
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.token.MediaHost
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import com.squareup.moshi.Moshi
import io.matthewnelson.crypto_common.clazzes.Password
import kotlinx.coroutines.flow.Flow
import java.io.File

abstract class NetworkQueryMemeServer {

    abstract fun askAuthentication(
        memeServerHost: MediaHost = MediaHost.DEFAULT,
    ): Flow<LoadResponse<MemeServerAuthenticationDto, ResponseError>>

    abstract fun signChallenge(
        challenge: AuthenticationChallenge,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>? = null,
    ): Flow<LoadResponse<MemeServerChallengeSigDto, ResponseError>>

    abstract fun verifyAuthentication(
        id: AuthenticationId,
        sig: AuthenticationSig,
        ownerPubKey: LightningNodePubKey,
        memeServerHost: MediaHost = MediaHost.DEFAULT,
    ): Flow<LoadResponse<MemeServerAuthenticationTokenDto, ResponseError>>

    abstract suspend fun getPaymentTemplates(
        authenticationToken: AuthenticationToken,
        memeServerHost: MediaHost = MediaHost.DEFAULT,
        moshi: Moshi,
    ): Flow<LoadResponse<List<PaymentTemplateDto>, ResponseError>>

    abstract suspend fun uploadAttachmentEncrypted(
        authenticationToken: AuthenticationToken,
        mediaType: MediaType,
        file: File,
        fileName: FileName?,
        password: Password,
        memeServerHost: MediaHost = MediaHost.DEFAULT,
    ): Response<PostMemeServerUploadDto, ResponseError>

    abstract suspend fun uploadAttachment(
        authenticationToken: AuthenticationToken,
        mediaType: MediaType,
        stream: InputStreamProvider,
        fileName: String,
        contentLength: Long?,
        memeServerHost: MediaHost = MediaHost.DEFAULT,
    ): Response<PostMemeServerUploadDto, ResponseError>
}
