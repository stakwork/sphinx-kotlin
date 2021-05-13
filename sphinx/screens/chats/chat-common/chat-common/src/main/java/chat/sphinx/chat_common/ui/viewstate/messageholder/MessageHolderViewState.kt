package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.isDirectPayment

sealed class MessageHolderViewState {

    abstract val message: Message
    abstract val background: HolderBackground
    abstract val statusHeader: HolderStatusHeader
    abstract val initialHolder: InitialHolderViewState

    abstract val directPayment: LayoutState.DirectPayment?

    class InComing(
        override val message: Message,
        override val background: HolderBackground.In,
        override val statusHeader: HolderStatusHeader.In,
        override val initialHolder: InitialHolderViewState,
    ): MessageHolderViewState() {
        override val directPayment: LayoutState.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
            if (message.type.isDirectPayment()) {
                LayoutState.DirectPayment(showSent = false, amount = message.amount)
            } else {
                null
            }
        }
    }

    class OutGoing(
        override val message: Message,
        override val background: HolderBackground.Out,
        override val statusHeader: HolderStatusHeader.Out,
    ): MessageHolderViewState() {
        override val initialHolder: InitialHolderViewState
            get() = InitialHolderViewState.None

        override val directPayment: LayoutState.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
            if (message.type.isDirectPayment()) {
                LayoutState.DirectPayment(showSent = true, amount = message.amount)
            } else {
                null
            }
        }

    }
}
