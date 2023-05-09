package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.wrapper_message.Message
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PinedMessageDataViewState: ViewState<PinedMessageDataViewState>() {
    object Idle: PinedMessageDataViewState()

    data class Data(
        val message: Message
    ): PinedMessageDataViewState()


}
