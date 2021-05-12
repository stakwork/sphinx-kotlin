package chat.sphinx.chat_common.ui.viewstate.messageholder

import androidx.annotation.MainThread
import chat.sphinx.chat_common.databinding.LayoutMessageHolderBinding
import chat.sphinx.wrapper_common.lightning.asFormattedString
import io.matthewnelson.android_feature_screens.util.goneIfFalse

@MainThread
@Suppress("NOTHING_TO_INLINE")
inline fun LayoutMessageHolderBinding.setDirectPaymentLayout(directPayment: LayoutState.DirectPayment?) {
    includeMessageHolderMessageTypes.includeMessageTypeDirectPayment.apply {
        if (directPayment == null) {
            root.goneIfFalse(false)
        } else {
            root.goneIfFalse(true)
            imageViewDirectPaymentSent.goneIfFalse(directPayment.showSent)
            imageViewDirectPaymentReceived.goneIfFalse(directPayment.showReceived)
            includeDirectPaymentAmountTextGroup.apply {
                textViewBoostAmount.text = directPayment.amount.asFormattedString()
                textViewBoostUnitLabel.text = directPayment.unitLabel
            }
        }
    }
}
