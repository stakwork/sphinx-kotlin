package chat.sphinx.subscription.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SubscriptionViewState: ViewState<SubscriptionViewState>() {
    object Idle: SubscriptionViewState()
}
