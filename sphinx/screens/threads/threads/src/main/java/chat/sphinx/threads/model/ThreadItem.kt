package chat.sphinx.threads.model

import androidx.annotation.IntRange
import chat.sphinx.chat_common.ui.viewstate.messageholder.ReplyUserHolder
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_message_media.FileName
import java.io.File

data class ThreadItem(
    val aliasAndColorKey: Pair<ContactAlias?, String?>,
    val photoUrl: PhotoUrl?,
    val date: String,
    val message: String,
    val highlightedTexts: List<Pair<String, IntRange>>,
    val usersReplies: List<ReplyUserHolder>?,
    val usersCount: Int,
    val repliesAmount: String,
    val lastReplyDate: String?,
    val uuid: String,
    val imageAttachment: Pair<String, File?>?,
    val videoAttachment: File?,
    val fileAttachment: FileAttachment?,
    val audioAttachment: Boolean?
)

data class FileAttachment(
    val fileName: FileName?,
    val fileSize: FileSize,
    val isPdf: Boolean,
    val pageCount: Int
)
