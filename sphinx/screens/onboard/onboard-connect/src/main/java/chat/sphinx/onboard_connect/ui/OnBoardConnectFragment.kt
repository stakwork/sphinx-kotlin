package chat.sphinx.onboard_connect.ui

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_connect.R
import chat.sphinx.onboard_connect.databinding.FragmentOnBoardConnectBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class OnBoardConnectFragment: SideEffectFragment<
        Context,
        OnBoardConnectSideEffect,
        OnBoardConnectViewState,
        OnBoardConnectViewModel,
        FragmentOnBoardConnectBinding
        >(R.layout.fragment_on_board_connect)
{
    override val viewModel: OnBoardConnectViewModel by viewModels()
    override val binding: FragmentOnBoardConnectBinding by viewBinding(FragmentOnBoardConnectBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderAndFooter()
        setupEditText()

        binding.apply {
            imageButtonScanner.setOnClickListener {
                viewModel.navigateToScanner()
            }

            textViewHeaderBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardConnect)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardConnect)
    }

    private fun setupEditText() {
        binding.editTextCodeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { code ->
                    viewModel.validateCode(code)
                }
            }
        })
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardConnectSideEffect) {
        if (sideEffect is OnBoardConnectSideEffect.FromScanner) {
            binding.editTextCodeInput.setText(sideEffect.value.value)
        } else {
            sideEffect.execute(binding.root.context)
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardConnectViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardConnectViewState.Idle -> { }
            is OnBoardConnectViewState.NewUser -> {
                binding.apply {
                    textViewOnboardConnectTitle.text = getString(R.string.on_board_new_user)
                    editTextCodeInput.hint = getString(R.string.on_board_connect_paste_connection_code)
                    imageButtonScanner.visible
                }
            }
            is OnBoardConnectViewState.ExistingUser -> {
                binding.apply {
                    textViewOnboardConnectTitle.text = getString(R.string.on_board_connect)
                    editTextCodeInput.hint = getString(R.string.on_board_connect_paste_keys)
                    imageButtonScanner.gone
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.submitButtonViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is OnBoardConnectSubmitButtonViewState.Disabled -> {
                        binding.apply {
                            buttonSubmit.background.colorFilter = PorterDuffColorFilter(
                                ContextCompat.getColor(
                                    root.context,
                                    R.color.on_board_submit_disabled_button
                                ),
                                PorterDuff.Mode.SRC_IN
                            )
                            buttonSubmit.setTextColor(ContextCompat.getColor(
                                root.context,
                                android.R.color.black
                            ))
                        }
                    }
                    is OnBoardConnectSubmitButtonViewState.Enabled -> {
                        binding.apply {
                            buttonSubmit.background.colorFilter = PorterDuffColorFilter(
                                ContextCompat.getColor(
                                    root.context,
                                    R.color.primaryBlue
                                ),
                                PorterDuff.Mode.SRC_IN
                            )
                            buttonSubmit.setTextColor(ContextCompat.getColor(
                                root.context,
                                android.R.color.white
                            ))
                        }
                    }
                }
            }
        }
    }
}