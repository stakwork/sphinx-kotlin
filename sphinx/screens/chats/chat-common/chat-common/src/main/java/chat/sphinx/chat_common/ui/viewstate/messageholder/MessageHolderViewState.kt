package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.wrapper_message.Message

sealed class MessageHolderViewState {

    abstract val message: Message
    abstract val background: HolderBackground
    abstract val initialHolder: InitialHolderViewState

    class InComing(
        override val message: Message,
        override val background: HolderBackground.In,
        override val initialHolder: InitialHolderViewState,
    ): MessageHolderViewState()

    class OutGoing(
        override val message: Message,
        override val background: HolderBackground.Out,
    ): MessageHolderViewState() {
        override val initialHolder: InitialHolderViewState
            get() = InitialHolderViewState.None
    }
}
