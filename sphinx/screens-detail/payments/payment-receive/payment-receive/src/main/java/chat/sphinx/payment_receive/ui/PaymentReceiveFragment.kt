package chat.sphinx.payment_receive.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.payment_receive.R
import chat.sphinx.payment_receive.databinding.FragmentPaymentReceiveBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class PaymentReceiveFragment: BaseFragment<
        PaymentReceiveViewState,
        PaymentReceiveViewModel,
        FragmentPaymentReceiveBinding
        >(R.layout.fragment_payment_receive)
{
    override val viewModel: PaymentReceiveViewModel by viewModels()
    override val binding: FragmentPaymentReceiveBinding by viewBinding(FragmentPaymentReceiveBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includePaymentReceiveHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.payment_receive_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: PaymentReceiveViewState) {
//        TODO("Not yet implemented")
    }
}
