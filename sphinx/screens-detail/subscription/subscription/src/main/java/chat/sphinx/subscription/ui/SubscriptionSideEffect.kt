package chat.sphinx.subscription.ui

import android.app.AlertDialog
import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.subscription.R
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class SubscriptionSideEffect: SideEffect<Context>() {
    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): SubscriptionSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }

    class AlertConfirmDeleteSubscription(
        private val callback: () -> Unit
    ): SubscriptionSideEffect() {

        override suspend fun execute(value: Context) {
            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setTitle(value.getString(R.string.delete_subscription))
            builder.setMessage(value.getString(R.string.are_you_sure_you_want_to_delete_subscription))
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }
}
