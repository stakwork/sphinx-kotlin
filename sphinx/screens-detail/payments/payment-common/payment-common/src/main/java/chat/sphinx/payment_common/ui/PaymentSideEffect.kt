package chat.sphinx.payment_common.ui

import android.app.AlertDialog
import android.view.HapticFeedbackConstants
import androidx.fragment.app.FragmentActivity
import chat.sphinx.payment_common.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

sealed class PaymentSideEffect: SideEffect<FragmentActivity>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): PaymentSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }

    class AlertConfirmPaymentSend(
        private val amount: Long,
        private val destination: String,
        private val callback: () -> Unit
    ): PaymentSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            val successMessage = value.getString(R.string.alert_confirm_payment_send_message, amount, destination)

            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.alert_confirm_payment_send_title))
            builder.setMessage(successMessage)
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    object ProduceHapticFeedback: PaymentSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            value.window.decorView.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }
}