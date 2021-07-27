package chat.sphinx.onboard_picture.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_picture.R
import chat.sphinx.onboard_picture.databinding.FragmentOnBoardPictureBinding
import chat.sphinx.onboard_picture.navigation.inviterData
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment

@AndroidEntryPoint
internal class OnBoardPictureFragment: SideEffectFragment<
        Context,
        OnBoardPictureSideEffect,
        OnBoardPictureViewState,
        OnBoardPictureViewModel,
        FragmentOnBoardPictureBinding
        >(R.layout.fragment_on_board_picture)
{
    private val args: OnBoardPictureFragmentArgs by navArgs()
    private val inviterData: OnBoardInviterData by lazy(LazyThreadSafetyMode.NONE) { args.inviterData }
    override val viewModel: OnBoardPictureViewModel by viewModels()
    override val binding: FragmentOnBoardPictureBinding by viewBinding(FragmentOnBoardPictureBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardPictureSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardPictureViewState) {
        @Exhaustive
        when (viewState) {
            OnBoardPictureViewState.Idle -> {}
        }
    }
}
