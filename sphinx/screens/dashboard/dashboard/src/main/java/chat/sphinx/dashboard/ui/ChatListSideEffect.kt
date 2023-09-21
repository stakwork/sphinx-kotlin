package chat.sphinx.dashboard.ui

import android.app.AlertDialog
import android.content.Context
import chat.sphinx.dashboard.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect
import java.util.Locale

sealed class ChatListSideEffect: SideEffect<Context>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = false
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }

    class AlertConfirmPayInvite(
        private val amount: Long,
        private val callback: () -> Unit
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            val successMessage = value.getString(R.string.alert_confirm_pay_invite_message, amount)

            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.alert_confirm_pay_invite_title))
            builder.setMessage(successMessage)
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    class AlertConfirmPayLightningPaymentRequest(
        private val amount: Long,
        private val memo: String = "",
        private val callback: () -> Unit
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            val memo = if (memo.isEmpty()) "-" else memo
            val successMessage = value.getString(R.string.alert_confirm_pay_invoice_message, amount, memo)

            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.alert_confirm_pay_invoice_title))
            builder.setMessage(successMessage)
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    class AlertConfirmDeleteInvite(
        private val callback: () -> Unit
    ): ChatListSideEffect() {
        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.alert_confirm_delete_invite_title))
            builder.setMessage(value.getString(R.string.alert_confirm_delete_invite_message))
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun String.toCapitalized(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.ROOT
            ) else it.toString()
        }
    }

}