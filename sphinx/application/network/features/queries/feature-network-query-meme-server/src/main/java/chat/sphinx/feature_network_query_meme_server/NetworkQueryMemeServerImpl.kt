package chat.sphinx.feature_network_query_meme_server

import chat.sphinx.concept_network_call.buildRequest
import chat.sphinx.concept_network_query_meme_server.NetworkQueryMemeServer
import chat.sphinx.concept_network_query_meme_server.model.*
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.feature_network_query_meme_server.model.MemeServerChallengeSigRelayResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_io_utils.InputStreamProvider
import chat.sphinx.wrapper_meme_server.*
import chat.sphinx.wrapper_message_media.FileName
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.token.MediaHost
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RequestSignature
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import com.squareup.moshi.*
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
import okio.BufferedSink
import okio.IOException
import okio.source
import okio.use
import org.cryptonode.jncryptor.AES256JNCryptorOutputStream
import java.io.File

class NetworkQueryMemeServerImpl(
    dispatchers: CoroutineDispatchers,
    private val networkRelayCall: NetworkRelayCall,
): NetworkQueryMemeServer(), CoroutineDispatchers by dispatchers {

    companion object {
        private const val FILE = "file"
        private const val NAME = "name"

        private const val MEME_SERVER_URL = "https://%s"

        private const val ENDPOINT_ASK_AUTHENTICATION = "$MEME_SERVER_URL/ask"
        private const val ENDPOINT_POST_ATTACHMENT_PRIVATE = "$MEME_SERVER_URL/file"
        private const val ENDPOINT_POST_ATTACHMENT_PUBLIC = "$MEME_SERVER_URL/public"
        private const val ENDPOINT_SIGNER = "/signer/%s"
        private const val ENDPOINT_VERIFY_AUTHENTICATION = "$MEME_SERVER_URL/verify?id=%s&sig=%s&pubkey=%s"
        private const val ENDPOINT_TEMPLATES = "$MEME_SERVER_URL/templates"
    }

    override fun askAuthentication(
        memeServerHost: MediaHost
    ): Flow<LoadResponse<MemeServerAuthenticationDto, ResponseError>> =
        networkRelayCall.get(
            url = String.format(ENDPOINT_ASK_AUTHENTICATION, memeServerHost.value),
            responseJsonClass = MemeServerAuthenticationDto::class.java,
        )

    override fun signChallenge(
        challenge: AuthenticationChallenge,
        relayData: Triple<Pair<AuthorizationToken, TransportToken?>, RequestSignature?, RelayUrl>?
    ): Flow<LoadResponse<MemeServerChallengeSigDto, ResponseError>> =
        networkRelayCall.relayGet(
            responseJsonClass = MemeServerChallengeSigRelayResponse::class.java,
            relayEndpoint = String.format(ENDPOINT_SIGNER, challenge.value),
            relayData = relayData,
        )

    override fun verifyAuthentication(
        id: AuthenticationId,
        sig: AuthenticationSig,
        ownerPubKey: LightningNodePubKey,
        memeServerHost: MediaHost,
    ): Flow<LoadResponse<MemeServerAuthenticationTokenDto, ResponseError>> =
        networkRelayCall.post(
            url = String.format(
                ENDPOINT_VERIFY_AUTHENTICATION,
                memeServerHost.value,
                id.value,
                sig.value,
                ownerPubKey.value
            ),
            responseJsonClass = MemeServerAuthenticationTokenDto::class.java,
            requestBodyJsonClass = Map::class.java,
            requestBody = mapOf(Pair("", "")),
        )

    override suspend fun getPaymentTemplates(
        authenticationToken: AuthenticationToken,
        memeServerHost: MediaHost,
        moshi: Moshi
    ): Flow<LoadResponse<List<PaymentTemplateDto>, ResponseError>> =
        networkRelayCall.getList(
            url = String.format(ENDPOINT_TEMPLATES, memeServerHost.value),
            headers = mapOf(Pair(authenticationToken.headerKey, authenticationToken.headerValue)),
            responseJsonClass = PaymentTemplateDto::class.java,
        )

    @OptIn(RawPasswordAccess::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun uploadAttachmentEncrypted(
        authenticationToken: AuthenticationToken,
        mediaType: MediaType,
        file: File,
        fileName: FileName?,
        password: Password,
        memeServerHost: MediaHost
    ): Response<PostMemeServerUploadDto, ResponseError> {

        val passwordCopy: CharArray = password.value.copyOf()
        val tmpFile = File(file.absolutePath + ".tmp")

        return try {
            // will throw an exception if the media type is invalid
            val type: okhttp3.MediaType = mediaType.value.toMediaType()

            val requestBuilder = networkRelayCall.buildRequest(
                url = String.format(ENDPOINT_POST_ATTACHMENT_PRIVATE, memeServerHost.value),
                headers = mapOf(Pair(authenticationToken.headerKey, authenticationToken.headerValue))
            )

            val fileBody: RequestBody = withContext(io) {

                val clearInputStream = file.inputStream()

                try {
                    if (tmpFile.exists() && !tmpFile.delete()) {
                        throw IOException("Temp file exists already and could not delete")
                    }

                    val encryptedOutput =
                        AES256JNCryptorOutputStream(
                            tmpFile.outputStream(),
                            passwordCopy
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

                tmpFile.asRequestBody(type)

            }

            val requestBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(NAME, type.type)
                .addFormDataPart(FILE, fileName?.value ?: file.name, fileBody)
                .build()

            requestBuilder.post(requestBody)

            val response = networkRelayCall.call(
                PostMemeServerUploadDto::class.java,
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
            passwordCopy.fill('*')
        }

    }

    @OptIn(RawPasswordAccess::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun uploadAttachment(
        authenticationToken: AuthenticationToken,
        mediaType: MediaType,
        stream: InputStreamProvider,
        fileName: String,
        contentLength: Long?,
        memeServerHost: MediaHost,
    ): Response<PostMemeServerUploadDto, ResponseError> {

        return try {
            val type: okhttp3.MediaType = mediaType.value.toMediaType()

            val requestBuilder = networkRelayCall.buildRequest(
                url = String.format(ENDPOINT_POST_ATTACHMENT_PUBLIC, memeServerHost.value),
                headers = mapOf(Pair(authenticationToken.headerKey, authenticationToken.headerValue)),
            )

            val dataBody: RequestBody = object : RequestBody() {

                override fun contentType(): okhttp3.MediaType {
                    return type
                }

                override fun contentLength(): Long {
                    return contentLength ?: super.contentLength()
                }

                override fun writeTo(sink: BufferedSink) {
                    stream.newInputStream().source().use { source -> sink.writeAll(source) }
                }
            }

            val requestBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(NAME, type.type)
                .addFormDataPart(FILE, fileName, dataBody)
                .build()

            requestBuilder.post(requestBody)

            val response = networkRelayCall.call(
                PostMemeServerUploadDto::class.java,
                requestBuilder.build(),
                useExtendedNetworkCallClient = true,
            )

            Response.Success(response)
        } catch (e: Exception) {
            Response.Error(
                ResponseError("Failed to upload file $fileName", e)
            )
        }
    }
}
