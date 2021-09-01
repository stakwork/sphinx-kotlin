package chat.sphinx.subscription.ui

import chat.sphinx.wrapper_subscription.Subscription
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SubscriptionViewState: ViewState<SubscriptionViewState>() {
    object Idle: SubscriptionViewState()

    object SavingSubscription: SubscriptionViewState()

    object SavingSubscriptionFailed: SubscriptionViewState()

    class SubscriptionLoaded(
        val subscription: Subscription
    ) : SubscriptionViewState()
}
