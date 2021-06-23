package chat.sphinx.payment_send.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.payment_send.R
import chat.sphinx.payment_send.databinding.FragmentPaymentSendBinding
import chat.sphinx.wrapper_contact.Contact
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class PaymentSendFragment: SideEffectFragment<
        FragmentActivity,
        PaymentSendSideEffect,
        PaymentSendViewState,
        PaymentSendViewModel,
        FragmentPaymentSendBinding
        >(R.layout.fragment_payment_send)
{

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: PaymentSendViewModel by viewModels()
    override val binding: FragmentPaymentSendBinding by viewBinding(FragmentPaymentSendBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            includePaymentSendHeader.apply {
                textViewDetailScreenHeaderName.text = getString(R.string.payment_send_header_name)
                textViewDetailScreenClose.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.closeDetailScreen()
                    }
                }
            }

            binding.buttonConfirm.setOnClickListener {
                viewModel.sendChatPayment(textViewContactName?.text?.toString())
            }
        }

        setupFooter()
        setupNumberPad()
    }

    private fun setupFooter() {
        val insetterActivity = (requireActivity() as InsetterActivity)
        insetterActivity.addNavigationBarPadding(binding.layoutConstraintPaymentSend)
    }

    private fun setupDestination(contact: Contact) {
        binding.apply {
            contact.alias?.value?.let { alias ->
                textViewContactName.text = alias
            }

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                contact.photoUrl?.value?.let { img ->
                    if (img.isNotEmpty()) {
                        imageLoader.load(
                            imageViewContactPicture,
                            img,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                .transformation(Transformation.CircleCrop)
                                .build()
                        )
                    }
                } ?: imageViewContactPicture
                    .setImageDrawable(
                        ContextCompat.getDrawable(
                            binding.root.context,
                            R.drawable.ic_profile_avatar_circle
                        )
                    )
            }
        }
    }

    private fun setupNumberPad() {
        binding.layoutAmountPad.let { numberPad ->
            numberPad.button0.setOnClickListener { addAmountCharacter('0') }
            numberPad.button1.setOnClickListener { addAmountCharacter( '1') }
            numberPad.button2.setOnClickListener { addAmountCharacter( '2') }
            numberPad.button3.setOnClickListener { addAmountCharacter( '3') }
            numberPad.button4.setOnClickListener { addAmountCharacter( '4') }
            numberPad.button5.setOnClickListener { addAmountCharacter( '5') }
            numberPad.button6.setOnClickListener { addAmountCharacter( '6') }
            numberPad.button7.setOnClickListener { addAmountCharacter( '7') }
            numberPad.button8.setOnClickListener { addAmountCharacter( '8') }
            numberPad.button9.setOnClickListener { addAmountCharacter( '9') }
            numberPad.buttonBackspace.setOnClickListener { removeLastCharacter() }
        }
    }

    private fun removeLastCharacter() {
        binding.textViewAmount.text?.let { amountString ->
            amountString.toString().dropLast(1)?.let { updatedAmountString ->
                viewModel.updateAmount(updatedAmountString)
            }
        }
    }

    private fun addAmountCharacter(c: Char) {
        binding.apply {
            textViewAmount.text?.let { amountString ->
                val updatedAmountString = "$amountString$c"
                viewModel.updateAmount(updatedAmountString)
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: PaymentSendViewState) {
        @Exhaustive
        when (viewState) {
            is PaymentSendViewState.Idle -> {}

            is PaymentSendViewState.KeySendPayment -> {
                binding.layoutConstraintFromContact.invisible
                binding.layoutConstraintMessage.invisible
            }

            is PaymentSendViewState.ChatPayment -> {
                binding.layoutConstraintFromContact.visible
                binding.layoutConstraintMessage.visible

                setupDestination(viewState.contact)
            }

            is PaymentSendViewState.ProcessingPayment -> {
                binding.buttonConfirm.isEnabled = false
                binding.confirmProgress.visible
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.amountViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is AmountViewState.Idle -> {}

                    is AmountViewState.AmountUpdated -> {
                        binding.textViewAmount.text = viewState.amountString
                        binding.buttonConfirm.goneIfFalse(viewState.amountString.isNotEmpty())
                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: PaymentSendSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
