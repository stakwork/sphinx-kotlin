package chat.sphinx.payment_receive.ui

import chat.sphinx.payment_receive.navigation.PaymentReceiveNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class PaymentReceiveViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: PaymentReceiveNavigator,
): BaseViewModel<PaymentReceiveViewState>(dispatchers, PaymentReceiveViewState.Idle)
{
}
