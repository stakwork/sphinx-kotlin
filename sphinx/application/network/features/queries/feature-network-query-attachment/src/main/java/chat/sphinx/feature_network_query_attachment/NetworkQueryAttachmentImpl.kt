package chat.sphinx.feature_network_query_attachment

import chat.sphinx.concept_network_call.buildRequest
import chat.sphinx.concept_network_query_attachment.NetworkQueryAttachment
import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentAuthenticationTokenDto
import chat.sphinx.concept_network_query_attachment.model.AttachmentChallengeSigDto
import chat.sphinx.concept_network_query_attachment.model.PostAttachmentUploadDto
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_attachment.model.SignChallengeRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_attachment.*
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.token.MediaHost
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.clazzes.Password
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.internal.closeQuietly
import okio.*
import org.cryptonode.jncryptor.AES256JNCryptorOutputStream
import java.io.File
import java.lang.Exception

class NetworkQueryAttachmentImpl(
    dispatchers: CoroutineDispatchers,
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryAttachment(), CoroutineDispatchers by dispatchers {

    companion object {
        private const val FILE = "file"
        private const val NAME = "name"

        private const val ATTACHMENTS_SERVER_URL = "https://%s"

        private const val ENDPOINT_ASK_AUTHENTICATION = "$ATTACHMENTS_SERVER_URL/ask"
        private const val ENDPOINT_POST_ATTACHMENT = "$ATTACHMENTS_SERVER_URL/file"
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

    @OptIn(RawPasswordAccess::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun uploadAttachment(
        authenticationToken: AuthenticationToken,
        mediaType: MediaType,
        file: File,
        password: Password?,
        memeServerHost: MediaHost
    ): Response<PostAttachmentUploadDto, ResponseError> {

        val passwordCopy: CharArray? = password?.value?.copyOf()
        val tmpFile = File(file.absolutePath + ".tmp")

        return try {
            // will throw an exception if the media type is invalid
            val type: okhttp3.MediaType = mediaType.value.toMediaType()

            val requestBuilder = networkRelayCall.buildRequest(
                url = String.format(ENDPOINT_POST_ATTACHMENT, memeServerHost.value),
                headers = mapOf(Pair(authenticationToken.headerKey, authenticationToken.headerValue))
            )

            val fileBody: RequestBody = passwordCopy?.let { nnPasswordCopy ->

                withContext(io) {

                    val clearInputStream = file.inputStream()

                    try {
                        if (tmpFile.exists() && !tmpFile.delete()) {
                            throw IOException("Temp file exists already and could not delete")
                        }

                        val encryptedOutput =
                            AES256JNCryptorOutputStream(
                                tmpFile.outputStream(),
                                nnPasswordCopy
                            )

                        encryptedOutput.use { outputStream ->
                            val buf = ByteArray(1024)
                            while (true) {
                                val read = clearInputStream.read(buf)
                                if (read == -1) break
                                outputStream.write(buf, 0, read)
                            }
                        }

                    } finally {
                        clearInputStream.closeQuietly()
                    }

                }

                tmpFile.asRequestBody(type)

            } ?: file.asRequestBody(type)

            val requestBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(NAME, type.type)
                .addFormDataPart(FILE, file.name, fileBody)
                .build()

            requestBuilder.post(requestBody)

            val response = networkRelayCall.call(
                PostAttachmentUploadDto::class.java,
                requestBuilder.build(),
                useExtendedNetworkCallClient = true
            )

            Response.Success(response)
        } catch (e: Exception) {
            Response.Error(
                ResponseError("Failed to upload file ${file.name}", e)
            )
        } finally {
            tmpFile.delete()
            passwordCopy?.fill('*')
        }

    }
}
