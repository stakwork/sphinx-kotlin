package chat.sphinx.threads.model

import chat.sphinx.chat_common.ui.viewstate.messageholder.ReplyUserHolder
import chat.sphinx.wrapper_common.PhotoUrl

data class ThreadItem(
    val userName: String,
    val userPic: PhotoUrl?,
    val date: String,
    val message: String,
    val usersReplies: List<ReplyUserHolder>?,
    val repliesAmount: String,
    val repliesExcess: String?,
    val lastReplyDate: String?
)
