package chat.sphinx.onboard_welcome.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_welcome.R
import chat.sphinx.onboard_welcome.databinding.FragmentOnBoardWelcomeBinding
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class OnBoardWelcomeFragment: BaseFragment<
        OnBoardWelcomeViewState,
        OnBoardWelcomeViewModel,
        FragmentOnBoardWelcomeBinding
        >(R.layout.fragment_on_board_welcome)
{

    override val viewModel: OnBoardWelcomeViewModel by viewModels()
    override val binding: FragmentOnBoardWelcomeBinding by viewBinding(FragmentOnBoardWelcomeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        binding.apply {
            buttonNewUser.setOnClickListener {
                viewModel.goToNewUserScreen()
            }

            buttonExistingUser.setOnClickListener {
                viewModel.goToExistingUserScreen()
            }
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardWelcome)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardWelcome)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardWelcomeViewState) {
        //TODO("Not yet implemented")
    }
}
