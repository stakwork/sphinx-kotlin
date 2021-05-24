package chat.sphinx.chat_common.ui.viewstate.messageholder

import android.view.Gravity
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.unit

internal sealed class LayoutState {

    data class MessageStatusHeader(
        val senderName: String?,
        val showSent: Boolean,

        // TODO: rework bolt icon when sending messages to be yellow (sending), red (failed), green(sent)
        val showBoltIcon: Boolean,

        val showLockIcon: Boolean,
        val timestamp: String,
    ) : LayoutState() {
        val showReceived: Boolean
            get() = !showSent
    }


    data class DeletedMessageDetails(
        val messageTextGravity: Int
    ) : LayoutState()


    sealed class Bubble : LayoutState() {

        data class Message(val text: String) : Bubble()

        data class DirectPayment(val showSent: Boolean, val amount: Sat) : Bubble() {
            val showReceived: Boolean
                get() = !showSent

            val unitLabel: String
                get() = amount.unit
        }
    }

}
