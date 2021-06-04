package chat.sphinx.payment_send.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.payment_send.R
import chat.sphinx.payment_send.databinding.FragmentPaymentSendBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class PaymentSendFragment: BaseFragment<
        PaymentSendViewState,
        PaymentSendViewModel,
        FragmentPaymentSendBinding
        >(R.layout.fragment_payment_send)
{
    override val viewModel: PaymentSendViewModel by viewModels()
    override val binding: FragmentPaymentSendBinding by viewBinding(FragmentPaymentSendBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includePaymentSendHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.payment_send_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: PaymentSendViewState) {
//        TODO("Not yet implemented")
    }
}
