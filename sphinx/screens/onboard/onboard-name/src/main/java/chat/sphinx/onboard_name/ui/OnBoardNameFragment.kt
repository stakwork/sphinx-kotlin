package chat.sphinx.onboard_name.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_name.R
import chat.sphinx.onboard_name.databinding.FragmentOnBoardNameBinding
import chat.sphinx.onboard_name.navigation.inviterData
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.updateViewState

@AndroidEntryPoint
internal class OnBoardNameFragment: SideEffectFragment<
        Context,
        OnBoardNameSideEffect,
        OnBoardNameViewState,
        OnBoardNameViewModel,
        FragmentOnBoardNameBinding
        >(R.layout.fragment_on_board_name)
{
    private val args: OnBoardNameFragmentArgs by navArgs()
    private val inviterData: OnBoardInviterData by lazy(LazyThreadSafetyMode.NONE) { args.inviterData }
    override val viewModel: OnBoardNameViewModel by viewModels()
    override val binding: FragmentOnBoardNameBinding by viewBinding(FragmentOnBoardNameBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        binding.buttonNext.setOnClickListener {
            viewModel.updateViewState(OnBoardNameViewState.Saving)

            val name = binding.signUpNameEditText.text?.trim().toString()

            viewModel.updateOwnerAlias(name)
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardName)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardName)
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardNameSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardNameViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardNameViewState.Idle -> {}
            is OnBoardNameViewState.Saving -> {
                binding.signUpNameProgressBar.visible
            }
            is OnBoardNameViewState.Error -> {
                binding.signUpNameProgressBar.gone
            }
        }
    }
}
