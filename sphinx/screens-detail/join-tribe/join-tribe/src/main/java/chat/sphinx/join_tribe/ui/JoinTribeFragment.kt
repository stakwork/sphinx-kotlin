package chat.sphinx.join_tribe.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.join_tribe.R
import chat.sphinx.join_tribe.databinding.FragmentJoinTribeBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class JoinTribeFragment: BaseFragment<
        JoinTribeViewState,
        JoinTribeViewModel,
        FragmentJoinTribeBinding
        >(R.layout.fragment_join_tribe)
{
    override val viewModel: JoinTribeViewModel by viewModels()
    override val binding: FragmentJoinTribeBinding by viewBinding(FragmentJoinTribeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.layoutScrollViewContent)

        binding.includeJoinTribeHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.join_tribe_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch { viewModel.navigator.closeDetailScreen() }
            }
        }

        viewModel.loadTribeData()
    }

    override suspend fun onViewStateFlowCollect(viewState: JoinTribeViewState) {
        binding.apply {
            @Exhaustive
            when (viewState) {
                is JoinTribeViewState.LoadingTribeInfo -> {
                    loadingTribeInfoContent.goneIfFalse(true)
                }
                is JoinTribeViewState.LoadingTribeFailed -> {
                    viewModel.navigator.closeDetailScreen()
                }
                is JoinTribeViewState.TribeInfo -> {
                    textViewTribeName.text = viewState.host
                    textViewTribeDescription.text = viewState.uuid
                    loadingTribeInfoContent.goneIfFalse(false)
                }
            }
        }
    }
}
