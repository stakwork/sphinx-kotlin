package chat.sphinx.payment_send.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.payment_send.R
import chat.sphinx.payment_send.databinding.FragmentPaymentSendBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class PaymentSendFragment: BaseFragment<
        PaymentSendViewState,
        PaymentSendViewModel,
        FragmentPaymentSendBinding
        >(R.layout.fragment_payment_send)
{
    override val viewModel: PaymentSendViewModel by viewModels()
    override val binding: FragmentPaymentSendBinding by viewBinding(FragmentPaymentSendBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: PaymentSendViewState) {
//        TODO("Not yet implemented")
    }
}
