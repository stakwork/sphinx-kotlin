package chat.sphinx.payment_common.ui

import android.app.AlertDialog
import android.view.HapticFeedbackConstants
import androidx.fragment.app.FragmentActivity
import chat.sphinx.payment_common.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class PaymentSideEffect: SideEffect<PaymentSideEffectFragment>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): PaymentSideEffect() {
        override suspend fun execute(value: PaymentSideEffectFragment) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value.paymentFragmentContext, msg)
        }
    }

    class AlertConfirmPaymentSend(
        private val amount: Long,
        private val destination: String,
        private val callback: () -> Unit
    ): PaymentSideEffect() {
        override suspend fun execute(value: PaymentSideEffectFragment) {
            val successMessage = String.format(value.paymentFragmentContext.getString(R.string.alert_confirm_payment_send_message), amount, destination)

            val builder = AlertDialog.Builder(value.paymentFragmentContext)
            builder.setTitle(value.paymentFragmentContext.getString(R.string.alert_confirm_payment_send_title))
            builder.setMessage(successMessage)
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    object ProduceHapticFeedback: PaymentSideEffect() {
        override suspend fun execute(value: PaymentSideEffectFragment) {
            value.fragmentActivity?.let {
                it.window.decorView.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        }
    }
}