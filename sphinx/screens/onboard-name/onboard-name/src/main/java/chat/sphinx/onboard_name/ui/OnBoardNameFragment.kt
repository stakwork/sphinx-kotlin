package chat.sphinx.onboard_name.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.onboard_name.R
import chat.sphinx.onboard_name.databinding.FragmentOnBoardNameBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.updateViewState
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class OnBoardNameFragment: SideEffectFragment<
        Context,
        OnBoardNameSideEffect,
        OnBoardNameViewState,
        OnBoardNameViewModel,
        FragmentOnBoardNameBinding
        >(R.layout.fragment_on_board_name)
{
    override val viewModel: OnBoardNameViewModel by viewModels()
    override val binding: FragmentOnBoardNameBinding by viewBinding(FragmentOnBoardNameBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(binding.root.context).addCallback(viewLifecycleOwner, requireActivity())

        binding.includeWelcomeScreen.buttonNext.setOnClickListener {
            viewModel.updateViewState(OnBoardNameViewState.Saving)

            val name = binding.includeWelcomeScreen.signUpNameEditText.text?.trim().toString()

            viewModel.updateOwner(name)
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardNameSideEffect) {
        // TODO("Not yet implemented")
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardNameViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardNameViewState.Idle -> {}
            is OnBoardNameViewState.Saving -> {
                binding.includeWelcomeScreen.signUpNameProgressBar.visible
            }
            is OnBoardNameViewState.Error -> {
                binding.includeWelcomeScreen.signUpNameProgressBar.gone
            }
        }
    }

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            return
        }
    }
}
