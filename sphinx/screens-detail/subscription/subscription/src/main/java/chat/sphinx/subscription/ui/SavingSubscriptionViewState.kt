package chat.sphinx.subscription.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class SavingSubscriptionViewState: ViewState<SavingSubscriptionViewState>() {
    object Idle: SavingSubscriptionViewState()

    object SavingSubscription: SavingSubscriptionViewState()

    object SavingSubscriptionFailed: SavingSubscriptionViewState()
}