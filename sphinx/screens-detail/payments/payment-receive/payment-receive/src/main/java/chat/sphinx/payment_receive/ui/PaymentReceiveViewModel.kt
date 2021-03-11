package chat.sphinx.payment_receive.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class PaymentReceiveViewModel @Inject constructor(

): BaseViewModel<PaymentReceiveViewState>(PaymentReceiveViewState.Idle)
{
}
