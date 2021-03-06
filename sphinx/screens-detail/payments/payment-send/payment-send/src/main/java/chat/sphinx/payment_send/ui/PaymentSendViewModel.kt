package chat.sphinx.payment_send.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class PaymentSendViewModel @Inject constructor(

): BaseViewModel<PaymentSendViewState>(PaymentSendViewState.Idle)
{
}
