package chat.sphinx.subscription.ui

import chat.sphinx.subscription.navigation.SubscriptionNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class SubscriptionViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: SubscriptionNavigator
): BaseViewModel<SubscriptionViewState>(dispatchers, SubscriptionViewState.Idle)
{
}
