package chat.sphinx.threads.model

import chat.sphinx.chat_common.ui.viewstate.messageholder.ReplyUserHolder
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_contact.ContactAlias

data class ThreadItem(
    val aliasAndColorKey: Pair<ContactAlias?, String?>,
    val photoUrl: PhotoUrl?,
    val date: String,
    val message: String,
    val usersReplies: List<ReplyUserHolder>?,
    val usersCount: Int,
    val repliesAmount: String,
    val lastReplyDate: String?,
    val uuid: String
)
