package chat.sphinx.onboard.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.onboard.R
import chat.sphinx.onboard.databinding.FragmentOnBoardBinding
import chat.sphinx.onboard.navigation.ToOnBoardView
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_toast_utils.show
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class OnBoardFragment: SideEffectFragment<
        Context,
        OnBoardSideEffect,
        OnBoardViewState,
        OnBoardViewModel,
        FragmentOnBoardBinding
        >(R.layout.fragment_on_board)
{
    override val viewModel: OnBoardViewModel by viewModels()
    override val binding: FragmentOnBoardBinding by viewBinding(FragmentOnBoardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(binding.root.context).addCallback(viewLifecycleOwner, requireActivity())
        arguments?.getString(ToOnBoardView.USER_INPUT)?.let { input ->
//            SphinxToastUtils().show(binding.root.context, input)
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardSideEffect) {
        // TODO("Not yet implemented")
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardViewState) {
        // TODO("Not yet implemented")
    }

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            lifecycleScope.launch {
                viewModel.navigator.popBackStack()
            }
        }
    }
}