package chat.sphinx.onboard_description.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_description.R
import chat.sphinx.onboard_description.databinding.FragmentOnBoardDescriptionBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class OnBoardDescriptionFragment: SideEffectFragment<
        Context,
        OnBoardDescriptionSideEffect,
        OnBoardDescriptionViewState,
        OnBoardDescriptionViewModel,
        FragmentOnBoardDescriptionBinding
        >(R.layout.fragment_on_board_description)
{
    override val viewModel: OnBoardDescriptionViewModel by viewModels()
    override val binding: FragmentOnBoardDescriptionBinding by viewBinding(FragmentOnBoardDescriptionBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()

        binding.buttonOnboardContinue.setOnClickListener {
            viewModel.nextScreen()
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardDescription)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardDescription)
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardDescriptionSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardDescriptionViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardDescriptionViewState.Idle -> { }
            is OnBoardDescriptionViewState.NewUser -> {
                binding.apply {
                    imageViewOnboardDescription.setImageDrawable(
                        ContextCompat.getDrawable(
                            binding.root.context,
                            R.drawable.new_user_description
                        )
                    )
                    textViewOnboardDescription.text = getString(R.string.on_board_paste_connection_code)
                }
            }
            is OnBoardDescriptionViewState.ExistingUser -> {
                binding.apply {
                    imageViewOnboardDescription.setImageDrawable(
                        ContextCompat.getDrawable(
                            binding.root.context,
                            R.drawable.existing_user_description
                        )
                    )
                    textViewOnboardDescription.text = getString(R.string.on_board_paste_keys)
                }
            }
        }
    }
}