package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.unit

sealed class LayoutState {

    data class DirectPayment(
        val showSent: Boolean,
        val amount: Sat
    ): LayoutState() {
        val showReceived: Boolean = !showSent
        val unitLabel: String = amount.unit
    }

}