package chat.sphinx.subscription.ui

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_subscription.Subscription
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SubscriptionViewState: ViewState<SubscriptionViewState>() {
    object Idle: SubscriptionViewState()

    class SubscriptionLoaded(
        val isActive: Boolean,
        val amount: Long,
        val timeInterval: String,
        val endNumber: Long?,
        val endDate: DateTime?,
    ) : SubscriptionViewState()
}
