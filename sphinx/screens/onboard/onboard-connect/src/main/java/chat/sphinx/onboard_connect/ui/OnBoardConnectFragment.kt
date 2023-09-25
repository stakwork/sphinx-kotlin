package chat.sphinx.onboard_connect.ui

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_signer_manager.SignerManager
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.menu_bottom_phone_signer_method.PhoneSignerMethodMenu
import chat.sphinx.menu_bottom_signer.BottomSignerMenu
import chat.sphinx.onboard_connect.R
import chat.sphinx.onboard_connect.databinding.FragmentOnBoardConnectBinding
import chat.sphinx.onboard_connect.viewstate.MnemonicDialogViewState
import chat.sphinx.onboard_connect.viewstate.MnemonicWordsViewState
import chat.sphinx.onboard_connect.viewstate.OnBoardConnectSubmitButtonViewState
import chat.sphinx.onboard_connect.viewstate.OnBoardConnectViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

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

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var signerManager: SignerManager

    private val bottomMenuSigner: BottomSignerMenu by lazy(LazyThreadSafetyMode.NONE) {
        BottomSignerMenu(
            onStopSupervisor,
            viewModel
        )
    }

    private val phoneSignerMethodMenu: PhoneSignerMethodMenu by lazy(LazyThreadSafetyMode.NONE) {
        PhoneSignerMethodMenu(
            onStopSupervisor,
            viewModel
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())
        setupHeaderAndFooter()
        setupEditText()
        setupSignerManager()

        binding.apply {
            imageButtonScanner.setOnClickListener {
                viewModel.navigateToScanner()
            }

            textViewHeaderBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }

            buttonSubmit.setOnClickListener {
                viewModel.continueToConnectingScreen(
                    binding.editTextCodeInput.text.toString()
                )

                hideKeyboardFrom(buttonSubmit.context, buttonSubmit)
            }

            includeLayoutMnemonicWords.includeLayoutMnemonicWordsDetail.apply {
                buttonCancel.setOnClickListener {
                    viewModel.mnemonicWordsViewStateContainer.updateViewState(MnemonicWordsViewState.Closed)
                }

                buttonConfirm.setOnClickListener {
                    val words = editTextMnemonic.text.toString()
                    viewModel.validateSeed(words)

                    hideKeyboardFrom(buttonConfirm.context, buttonConfirm)
                }
            }
        }

        bottomMenuSigner.initialize(
            R.string.bottom_menu_signer_header_text,
            binding.includeLayoutMenuBottomSigner,
            viewLifecycleOwner
        )

        phoneSignerMethodMenu.initialize(
            R.string.bottom_menu_phone_signer_method_header_text,
            binding.includeLayoutMenuBottomPhoneSignerMethod,
            viewLifecycleOwner
        )
    }

    private fun setupSignerManager(){
        viewModel.setSignerManager(signerManager)
    }

    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showKeyboardFrom(context: Context, view: View) {
        view.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardConnect)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardConnect)
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ): OnBackPressedCallback(true) {

        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }

        override fun handleOnBackPressed() {
            when {
                (viewModel.mnemonicWordsViewStateContainer.value is MnemonicWordsViewState.Open) -> {
                    viewModel.mnemonicWordsViewStateContainer.updateViewState(MnemonicWordsViewState.Closed)
                }
                else -> {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.popBackStack()
                    }
                }
            }
        }
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
            val editTextCodeInput = binding.editTextCodeInput
            editTextCodeInput.setText(sideEffect.value.value)
            hideKeyboardFrom(editTextCodeInput.context, editTextCodeInput)
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

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.mnemonicWordsViewStateContainer.collect { viewState ->
                binding.includeLayoutMnemonicWords.apply {
                    when (viewState) {
                        is MnemonicWordsViewState.Open -> {
                            val editText = binding
                                .includeLayoutMnemonicWords
                                .includeLayoutMnemonicWordsDetail
                                .editTextMnemonic
                            showKeyboardFrom(editText.context, editText)
                        }
                        is MnemonicWordsViewState.Closed -> {
                            val cancelButton = binding
                                .includeLayoutMnemonicWords
                                .includeLayoutMnemonicWordsDetail
                                .buttonCancel
                            hideKeyboardFrom(cancelButton.context, cancelButton)
                        }
                    }
                    root.setTransitionDuration(300)
                    viewState.transitionToEndSet(root)
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.mnemonicDialogViewStateContainer.collect { viewState ->
                binding.includeLayoutMnemonicWords.includeLayoutMnemonicWordsDetail.apply {
                    when (viewState) {
                        is MnemonicDialogViewState.Idle -> {
                            layoutConstraintEnterWordsContainer.visible
                            layoutConstraintLoadingContainer.gone
                        }
                        is MnemonicDialogViewState.Loading -> {
                            layoutConstraintLoadingContainer.visible
                            layoutConstraintEnterWordsContainer.gone
                        }
                    }
                }
            }
        }

    }
}