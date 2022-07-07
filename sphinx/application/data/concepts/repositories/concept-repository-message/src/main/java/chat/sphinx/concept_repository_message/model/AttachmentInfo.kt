package chat.sphinx.concept_repository_message.model

import chat.sphinx.wrapper_message_media.FileName
import chat.sphinx.wrapper_message_media.MediaType
import java.io.File

data class AttachmentInfo(
    val file: File,
    val mediaType: MediaType,
    val fileName: FileName?,
    val isLocalFile: Boolean,
)
