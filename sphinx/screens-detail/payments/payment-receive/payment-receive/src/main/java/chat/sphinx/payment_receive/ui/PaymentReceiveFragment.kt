package chat.sphinx.payment_receive.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.payment_receive.R
import chat.sphinx.payment_receive.databinding.FragmentPaymentReceiveBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class PaymentReceiveFragment: BaseFragment<
        PaymentReceiveViewState,
        PaymentReceiveViewModel,
        FragmentPaymentReceiveBinding
        >(R.layout.fragment_payment_receive)
{
    override val viewModel: PaymentReceiveViewModel by viewModels()
    override val binding: FragmentPaymentReceiveBinding by viewBinding(FragmentPaymentReceiveBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: PaymentReceiveViewState) {
//        TODO("Not yet implemented")
    }
}
