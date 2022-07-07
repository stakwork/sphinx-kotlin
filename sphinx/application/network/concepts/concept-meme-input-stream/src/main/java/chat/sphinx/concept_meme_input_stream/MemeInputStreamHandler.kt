package chat.sphinx.concept_meme_input_stream

import chat.sphinx.wrapper_meme_server.AuthenticationToken
import chat.sphinx.wrapper_message_media.FileName
import chat.sphinx.wrapper_message_media.MediaKeyDecrypted
import java.io.InputStream


abstract class MemeInputStreamHandler {
    abstract suspend fun retrieveMediaInputStream(
        url: String,
        authenticationToken: AuthenticationToken?,
        mediaKeyDecrypted: MediaKeyDecrypted?
    ): Pair<InputStream?, FileName?>?
}
