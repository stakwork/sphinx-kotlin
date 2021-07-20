package chat.sphinx.tribe_detail.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.tribe_detail.R
import chat.sphinx.tribe_detail.databinding.FragmentTribeDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class TribeDetailFragment: BaseFragment<
        TribeDetailViewState,
        TribeDetailViewModel,
        FragmentTribeDetailBinding
        >(R.layout.fragment_tribe_detail)
{
    override val viewModel: TribeDetailViewModel by viewModels()
    override val binding: FragmentTribeDetailBinding by viewBinding(FragmentTribeDetailBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeTribeDetailHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.tribe_detail_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(detailViewState: TribeDetailViewState) {
//        TODO("Not yet implemented")
    }
}
