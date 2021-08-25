package chat.sphinx.subscription.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.subscription.R
import chat.sphinx.subscription.databinding.FragmentSubscriptionBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class SubscriptionFragment: BaseFragment<
        SubscriptionViewState,
        SubscriptionViewModel,
        FragmentSubscriptionBinding
        >(R.layout.fragment_subscription)
{
    override val viewModel: SubscriptionViewModel by viewModels()
    override val binding: FragmentSubscriptionBinding by viewBinding(FragmentSubscriptionBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeSubscriptionHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.subscription_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch { viewModel.navigator.closeDetailScreen() }
            }
            textViewDetailScreenHeaderNavBack.apply {
                visible
                setOnClickListener {
                    lifecycleScope.launch { viewModel.navigator.popBackStack() }
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: SubscriptionViewState) {
//        TODO("Not yet implemented")
    }
}
