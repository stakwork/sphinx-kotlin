package chat.sphinx.meme_input_stream

import chat.sphinx.concept_network_client_crypto.CryptoHeader
import chat.sphinx.concept_network_client_crypto.CryptoScheme
import chat.sphinx.wrapper_meme_server.AuthenticationToken
import chat.sphinx.wrapper_meme_server.headerKey
import chat.sphinx.wrapper_meme_server.headerValue
import chat.sphinx.wrapper_message_media.FileName
import chat.sphinx.wrapper_message_media.MediaKeyDecrypted
import chat.sphinx.wrapper_message_media.toFileName
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.InputStream


internal data class MemeInputStreamRetriever(
    val url: HttpUrl,
    val authenticationToken: AuthenticationToken?,
    val mediaKeyDecrypted: MediaKeyDecrypted?
) {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getMemeInputStream(
        dispatchers: CoroutineDispatchers,
        okHttpClient: OkHttpClient
    ): Pair<InputStream?, FileName?> {
        val request = Request.Builder().apply {
            url(url)
            authenticationToken?.let {
                addHeader(authenticationToken.headerKey, authenticationToken.headerValue)
            }

            mediaKeyDecrypted?.value?.let { key ->
                val header = CryptoHeader.Decrypt.Builder()
                    .setScheme(CryptoScheme.Decrypt.JNCryptor)
                    .setPassword(key)
                    .build()

                addHeader(header.key, header.value)
            }
        }.build()

        var response: Response?

        withContext(dispatchers.io) {
            response =
                try {
                    okHttpClient.newCall(request).execute()
                } catch (e: Exception) {
                    null
                }

            if (response?.isSuccessful == null) {
                response?.body?.closeQuietly()
            }
        }

        val inputStream = if (response?.isSuccessful == false) {
            null
        } else {
            response?.body?.source()?.inputStream()
        }

        response?.header("Content-Disposition")?.let { contentDisposition ->
            if (contentDisposition.contains("filename=")) {
                return Pair(
                    inputStream,
                    contentDisposition
                        .replaceBefore("filename=", "")
                        .replace("filename=", "")
                        .toFileName()
                )
            }
        }

        return Pair(inputStream, null)
    }
}
