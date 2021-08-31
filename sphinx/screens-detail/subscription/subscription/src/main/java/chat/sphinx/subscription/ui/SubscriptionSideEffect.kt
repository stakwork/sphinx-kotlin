package chat.sphinx.subscription.ui

import android.app.AlertDialog
import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
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
            val builder = AlertDialog.Builder(value)
            builder.setTitle("Delete subscription to contact")
            builder.setMessage("Are yuo sure you want to delete this subscription")
            builder.setNegativeButton(android.R.string.cancel) { _,_ -> }
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            builder.show()
        }
    }
}
