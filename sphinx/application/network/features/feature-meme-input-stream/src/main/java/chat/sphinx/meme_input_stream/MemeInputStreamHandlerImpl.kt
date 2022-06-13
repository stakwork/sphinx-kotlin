package chat.sphinx.meme_input_stream

import chat.sphinx.concept_meme_input_stream.MemeInputStreamHandler
import chat.sphinx.concept_network_client_cache.NetworkClientCache
import chat.sphinx.wrapper_meme_server.AuthenticationToken
import chat.sphinx.wrapper_message_media.FileName
import chat.sphinx.wrapper_message_media.MediaKeyDecrypted
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.InputStream

class MemeInputStreamHandlerImpl(
    private val dispatchers: CoroutineDispatchers,
    private val networkClientCache: NetworkClientCache,
) : MemeInputStreamHandler() {

    override suspend fun retrieveMediaInputStream(
        url: String,
        authenticationToken: AuthenticationToken?,
        mediaKeyDecrypted: MediaKeyDecrypted?
    ): Pair<InputStream?, FileName?>? {
        return url.toHttpUrlOrNull()?.let { httpUrl ->
            MemeInputStreamRetriever(
                httpUrl,
                authenticationToken,
                mediaKeyDecrypted
            ).getMemeInputStream(dispatchers, networkClientCache.getCachingClient())
        }
    }
}
