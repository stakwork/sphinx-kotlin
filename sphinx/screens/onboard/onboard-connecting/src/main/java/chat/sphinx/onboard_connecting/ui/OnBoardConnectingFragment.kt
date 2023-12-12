package chat.sphinx.onboard_connecting.ui

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.authentication_resources.databinding.LayoutAuthenticationBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_signer_manager.SignerManager
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.onboard_connecting.R
import chat.sphinx.onboard_connecting.databinding.FragmentOnBoardConnectingBinding
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class OnBoardConnectingFragment: MotionLayoutFragment<
        Any,
        Context,
        OnBoardConnectingSideEffect,
        OnBoardConnectingViewState,
        OnBoardConnectingViewModel,
        FragmentOnBoardConnectingBinding
        >(R.layout.fragment_on_board_connecting)
{
    override val viewModel: OnBoardConnectingViewModel by viewModels()
    override val binding: FragmentOnBoardConnectingBinding by viewBinding(FragmentOnBoardConnectingBinding::bind)

    private val bindingAuthentication: LayoutAuthenticationBinding by viewBinding(LayoutAuthenticationBinding::bind)

    @Inject
    lateinit var imageLoaderInj: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var signerManager: SignerManager

    private val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OnBackPress(binding.root.context).addCallback(viewLifecycleOwner, requireActivity())

        setupHeaderAndFooter()
        setupSignerManager()

        lifecycleScope.launch {
            imageLoader.load(
                binding.imageViewOnBoardConnecting,
                R.drawable.connecting,
            )
        }
    }

    private fun setupSignerManager(){
        viewModel.setSignerManager(signerManager)
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutMotionOnBoardConnecting)
    }

    private var doublePressBackJob: Job? = null
    private inner class OnBackPress(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            viewModel.currentViewState.let { viewState ->
                if (viewState is OnBoardConnectingViewState.Set2_DecryptKeys) {
                    if (viewState.inputLock && doublePressBackJob?.isActive != true) {
                        doublePressBackJob = lifecycleScope.launch(viewModel.mainImmediate) {
                            SphinxToastUtils().show(
                                binding.root.context,
                                io.matthewnelson.android_feature_screens.R.string.close_app_double_tap_toast_msg
                            )
                            delay(2_000)
                        }
                    } else {
                        lifecycleScope.launch(viewModel.mainImmediate) {
                            viewModel.navigator.popBackStack()
                        }
                    }
                } else {
                    super.handleOnBackPressed()
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardConnectingSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardConnectingViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardConnectingViewState.Connecting -> {
                bindingAuthentication.layoutPinPad.let { pinPad ->
                    pinPad.button0.setOnClickListener(null)
                    pinPad.button1.setOnClickListener(null)
                    pinPad.button2.setOnClickListener(null)
                    pinPad.button3.setOnClickListener(null)
                    pinPad.button4.setOnClickListener(null)
                    pinPad.button5.setOnClickListener(null)
                    pinPad.button6.setOnClickListener(null)
                    pinPad.button7.setOnClickListener(null)
                    pinPad.button8.setOnClickListener(null)
                    pinPad.button9.setOnClickListener(null)
                    pinPad.buttonBackspace.setOnClickListener(null)
                    pinPad.button0.isEnabled = false
                    pinPad.button1.isEnabled = false
                    pinPad.button2.isEnabled = false
                    pinPad.button3.isEnabled = false
                    pinPad.button4.isEnabled = false
                    pinPad.button5.isEnabled = false
                    pinPad.button6.isEnabled = false
                    pinPad.button7.isEnabled = false
                    pinPad.button8.isEnabled = false
                    pinPad.button9.isEnabled = false
                    pinPad.buttonBackspace.isEnabled = false
                }
            }

            is OnBoardConnectingViewState.Transition_Set2_DecryptKeys -> {
                delay(500L) // keyboard close
                setTransitionListener(binding.layoutMotionOnBoardConnecting)
                viewState.transitionToEndSet(binding.layoutMotionOnBoardConnecting)
            }

            is OnBoardConnectingViewState.Set2_DecryptKeys -> {
                updatePinHints(viewState)
                updateProgressBar(viewState)

                bindingAuthentication.layoutPinPad.let { pinPad ->
                    pinPad.button0.isEnabled = true
                    pinPad.button1.isEnabled = true
                    pinPad.button2.isEnabled = true
                    pinPad.button3.isEnabled = true
                    pinPad.button4.isEnabled = true
                    pinPad.button5.isEnabled = true
                    pinPad.button6.isEnabled = true
                    pinPad.button7.isEnabled = true
                    pinPad.button8.isEnabled = true
                    pinPad.button9.isEnabled = true
                    pinPad.buttonBackspace.isEnabled = true
                    pinPad.button0.setOnClickListener { addCharacter(viewState, '0') }
                    pinPad.button1.setOnClickListener { addCharacter(viewState, '1') }
                    pinPad.button2.setOnClickListener { addCharacter(viewState, '2') }
                    pinPad.button3.setOnClickListener { addCharacter(viewState, '3') }
                    pinPad.button4.setOnClickListener { addCharacter(viewState, '4') }
                    pinPad.button5.setOnClickListener { addCharacter(viewState, '5') }
                    pinPad.button6.setOnClickListener { addCharacter(viewState, '6') }
                    pinPad.button7.setOnClickListener { addCharacter(viewState, '7') }
                    pinPad.button8.setOnClickListener { addCharacter(viewState, '8') }
                    pinPad.button9.setOnClickListener { addCharacter(viewState, '9') }
                    pinPad.buttonBackspace.setOnClickListener {

                        produceHapticFeedback()

                        if (viewState.pinWriter.size() > 0 && !viewState.inputLock) {
                            val before = viewState.pinWriter.toCharArray()
                            viewState.pinWriter.reset()

                            for ((index, c) in before.withIndex()) {
                                if (index != before.size - 1) {
                                    viewState.pinWriter.append(c)
                                }
                            }

                            before.fill('0')
                            updatePinHints(viewState)
                        }
                    }
                }
            }
        }
    }

    private fun addCharacter(viewState: OnBoardConnectingViewState.Set2_DecryptKeys, c: Char) {
        produceHapticFeedback()
        if (viewState.pinWriter.size() < 7 && !viewState.inputLock) {
            viewState.pinWriter.append(c)
            updatePinHints(viewState)
            if (viewState.pinWriter.size() == 6) {
                viewState.inputLock = true
                updateProgressBar(viewState)

                viewModel.decryptInput(viewState)
            }
        }
    }

    private fun produceHapticFeedback() {
        requireActivity().window.decorView.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    private fun updatePinHints(viewState: OnBoardConnectingViewState.Set2_DecryptKeys) {
        val pinSize = viewState.pinWriter.size()
        bindingAuthentication.layoutPinHint.let { pinHint ->
            pinHint.imageViewPinHintFilled1.invisibleIfFalse(pinSize > 0)
            pinHint.imageViewPinHintFilled2.invisibleIfFalse(pinSize > 1)
            pinHint.imageViewPinHintFilled3.invisibleIfFalse(pinSize > 2)
            pinHint.imageViewPinHintFilled4.invisibleIfFalse(pinSize > 3)
            pinHint.imageViewPinHintFilled5.invisibleIfFalse(pinSize > 4)
            pinHint.imageViewPinHintFilled6.invisibleIfFalse(pinSize > 5)
        }
    }

    private fun updateProgressBar(viewState: OnBoardConnectingViewState.Set2_DecryptKeys) {
        bindingAuthentication.progressBar.invisibleIfFalse(viewState.inputLock)
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
        when (currentId) {
            OnBoardConnectingViewState.Transition_Set2_DecryptKeys.END_SET_ID -> {
                removeTransitionListener(binding.layoutMotionOnBoardConnecting)
                viewModel.updateViewState(
                    OnBoardConnectingViewState.Set2_DecryptKeys(
                        try {
                            (viewModel.currentViewState as OnBoardConnectingViewState.Transition_Set2_DecryptKeys).restoreCode
                        } catch (e: ClassCastException) {
                            // "Should" never happen, but if it does, this is the only other
                            // view state that contains set2's resource ID.
                            (viewModel.currentViewState as OnBoardConnectingViewState.Set2_DecryptKeys).restoreCode
                        }
                    )
                )
            }
        }
    }

    override fun onViewCreatedRestoreMotionScene(viewState: OnBoardConnectingViewState, binding: FragmentOnBoardConnectingBinding) {
        viewState.restoreMotionScene(binding.layoutMotionOnBoardConnecting)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionOnBoardConnecting)
    }
}