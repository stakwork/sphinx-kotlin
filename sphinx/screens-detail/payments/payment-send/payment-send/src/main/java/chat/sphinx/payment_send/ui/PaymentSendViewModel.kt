package chat.sphinx.payment_send.ui

import chat.sphinx.payment_send.navigation.PaymentSendNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class PaymentSendViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: PaymentSendNavigator,
): BaseViewModel<PaymentSendViewState>(dispatchers, PaymentSendViewState.Idle)
{
}
