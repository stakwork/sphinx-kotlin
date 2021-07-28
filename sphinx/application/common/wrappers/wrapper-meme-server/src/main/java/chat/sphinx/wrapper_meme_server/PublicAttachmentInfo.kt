package chat.sphinx.wrapper_meme_server

import chat.sphinx.wrapper_io_utils.InputStreamProvider
import chat.sphinx.wrapper_message_media.MediaType

data class PublicAttachmentInfo(
    val stream: InputStreamProvider,
    val mediaType: MediaType,
    val fileName: String,
    val contentLength: Long? = null,
)