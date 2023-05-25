package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_message.Message
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PinedMessageDataViewState: ViewState<PinedMessageDataViewState>() {
    object Idle: PinedMessageDataViewState()

    data class Data(
        val message: Message,
        val messageContent: String,
        val isOwnTribe: Boolean,
        val senderAlias: String,
        val senderPic: PhotoUrl?,
        val senderColorKey: String
    ): PinedMessageDataViewState()


}
