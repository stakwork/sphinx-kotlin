package chat.sphinx.subscription.ui

import chat.sphinx.subscription.navigation.SubscriptionNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import java.util.*
import javax.inject.Inject

@HiltViewModel
internal class SubscriptionViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: SubscriptionNavigator
): BaseViewModel<SubscriptionViewState>(dispatchers, SubscriptionViewState.Idle)
{

    fun saveSubscription(
        amount: Int?,
        cron: String?,
        endDate: Date?
    ) {
        if (amount == null) {
            // TODO: Let user know amount is required
            return
        }

        if (cron == null) {
            // TODO: Let user know interval is required
            return
        }
    }
}
