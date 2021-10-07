package chat.sphinx.onboard_desktop.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_desktop.R
import chat.sphinx.onboard_desktop.databinding.FragmentOnBoardDesktopBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class OnBoardDesktopFragment: BaseFragment<
        OnBoardDesktopViewState,
        OnBoardDesktopViewModel,
        FragmentOnBoardDesktopBinding
        >(R.layout.fragment_on_board_desktop)
{
    override val viewModel: OnBoardDesktopViewModel by viewModels()
    override val binding: FragmentOnBoardDesktopBinding by viewBinding(FragmentOnBoardDesktopBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardDesktop)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardDesktop)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardDesktopViewState) {
        //TODO implement state flow collector
    }
}