package chat.sphinx.feature_network_client

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_client_crypto.CryptoScheme
import chat.sphinx.concept_network_client_crypto.isCryptoHeader
import chat.sphinx.concept_network_client_crypto.retrieveCryptoScheme
import okhttp3.*
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.cryptonode.jncryptor.AES256JNCryptorInputStream
import java.io.IOException
import kotlin.jvm.Throws

@Suppress("NOTHING_TO_INLINE")
private inline fun CryptoInterceptor.buildInvalidResponse(
    request: Request,
    failureMessage: String,
): Response =
    Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(400)
        .message(failureMessage)
        .body(failureMessage.toResponseBody(null))
        .build()

/**
 * Intercepts a network request where a [chat.sphinx.concept_network_client_crypto.CryptoHeader]
 * is present, and utilizes the provided header value to encrypt/decrypt the
 * [RequestBody]/[ResponseBody] for the declared [CryptoScheme] within the header key.
 *
 * The headers (if present) are stripped from the request prior to execution as to not leak
 * sensitive data.
 * */
class CryptoInterceptor: Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val cryptoHeaders: List<Pair<String, String>> =
            request.headers.filter { it.first.isCryptoHeader }

        cryptoHeaders.firstOrNull()?.let { cryptoHeader ->
            val builder = request.newBuilder()

            // Strip _all_ CryptoHeaders
            for (header in cryptoHeaders) {
                builder.removeHeader(header.first)
            }

            val scheme: CryptoScheme = try {
                cryptoHeader.first.retrieveCryptoScheme()
                    ?: throw IllegalArgumentException(
                        """
                            CryptoScheme unexpectedly returned null
                            when processing the request header
                        """.trimIndent()
                    )
            } catch (e: IllegalArgumentException) {
                return buildInvalidResponse(
                    request = request,
                    failureMessage = e.message ?: "Invalid CryptoScheme retrieval from headers",
                )
            }

            return handleRequest(scheme, cryptoHeader.second, chain, builder.build())
        } ?: return chain.proceed(request)
    }

    @Throws(IOException::class)
    private fun handleRequest(
        scheme: CryptoScheme,
        password: String,
        chain: Interceptor.Chain,
        request: Request
    ): Response {

        @Exhaustive
        when (scheme) {
            is CryptoScheme.Decrypt -> {
                val response: Response = chain.proceed(request)

                if (!response.isSuccessful) {
                    return response
                }

                val responseBody: ResponseBody = response.body ?: return response

                @Exhaustive
                when (scheme) {
                    CryptoScheme.Decrypt.JNCryptor -> {

                        if (password.isEmpty()) {
                            return response
                        }

                        val stream = AES256JNCryptorInputStream(
                            responseBody.byteStream(),
                            password.toCharArray()
                        )

                        val newBuffer: BufferedSource = Buffer().readFrom(stream)

                        return response.newBuilder()
                            .body(newBuffer.asResponseBody(responseBody.contentType()))
                            .build()
                    }
                }
            }
            CryptoScheme.Encrypt.JNCryptor -> {
                // TODO: Build out sending
                throw IOException("Encryption is not yet supported for sending")
            }
        }
    }
}
