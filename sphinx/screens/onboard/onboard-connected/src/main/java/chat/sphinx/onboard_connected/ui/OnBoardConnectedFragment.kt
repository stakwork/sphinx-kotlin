package chat.sphinx.onboard_connected.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_connected.R
import chat.sphinx.onboard_connected.databinding.FragmentOnBoardConnectedBinding
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class OnBoardConnectedFragment: BaseFragment<
        OnBoardConnectedViewState,
        OnBoardConnectedViewModel,
        FragmentOnBoardConnectedBinding
        >(R.layout.fragment_on_board_connected)
{
    override val viewModel: OnBoardConnectedViewModel by viewModels()
    override val binding: FragmentOnBoardConnectedBinding by viewBinding(FragmentOnBoardConnectedBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        setupHeaderAndFooter()

        binding.apply {
            buttonContinue.setOnClickListener {
                viewModel.continueToDashboardScreen()
            }
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardConnected)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardConnected)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardConnectedViewState) {
        //TODO implement state flow collector
    }
}