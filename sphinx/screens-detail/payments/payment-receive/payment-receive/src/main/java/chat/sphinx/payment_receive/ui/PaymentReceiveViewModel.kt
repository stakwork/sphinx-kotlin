package chat.sphinx.payment_receive.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class PaymentReceiveViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
): BaseViewModel<PaymentReceiveViewState>(dispatchers, PaymentReceiveViewState.Idle)
{
}
