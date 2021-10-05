package chat.sphinx.onboard_welcome.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_welcome.R
import chat.sphinx.onboard_welcome.databinding.FragmentOnBoardWelcomeBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment

@AndroidEntryPoint
internal class OnBoardWelcomeFragment: SideEffectFragment<
        Context,
        OnBoardWelcomeSideEffect,
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

    override suspend fun onSideEffectCollect(sideEffect: OnBoardWelcomeSideEffect) {
        TODO("Not yet implemented")
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardWelcomeViewState) {
        TODO("Not yet implemented")
    }
}
