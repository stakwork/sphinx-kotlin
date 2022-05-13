package chat.sphinx.payment_send.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.detail_resources.databinding.LayoutDetailScreenHeaderBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.payment_common.databinding.LayoutConstraintAmountBinding
import chat.sphinx.payment_common.databinding.LayoutConstraintConfirmButtonBinding
import chat.sphinx.payment_common.databinding.LayoutConstraintFromContactBinding
import chat.sphinx.payment_common.databinding.LayoutConstraintMessageBinding
import chat.sphinx.payment_common.ui.PaymentFragment
import chat.sphinx.payment_common.ui.PaymentSideEffect
import chat.sphinx.payment_common.ui.viewstate.send.PaymentSendViewState
import chat.sphinx.payment_send.R
import chat.sphinx.payment_send.databinding.FragmentPaymentSendBinding
import chat.sphinx.resources.databinding.LayoutAmountPadBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class PaymentSendFragment: PaymentFragment<
        FragmentPaymentSendBinding,
        PaymentSendFragmentArgs,
        PaymentSendViewModel,
        PaymentSendViewState
        >(R.layout.fragment_payment_send)
{
    override val viewModel: PaymentSendViewModel by viewModels()
    override val binding: FragmentPaymentSendBinding by viewBinding(FragmentPaymentSendBinding::bind)

    override val headerBinding: LayoutDetailScreenHeaderBinding by viewBinding(
        LayoutDetailScreenHeaderBinding::bind, R.id.include_payment_send_header
    )

    override val contactBinding: LayoutConstraintFromContactBinding by viewBinding(
        LayoutConstraintFromContactBinding::bind, R.id.include_constraint_from_contact
    )

    override val amountBinding: LayoutConstraintAmountBinding by viewBinding(
        LayoutConstraintAmountBinding::bind, R.id.include_constraint_amount
    )

    override val amountPadBinding: LayoutAmountPadBinding by viewBinding(
        LayoutAmountPadBinding::bind, R.id.include_amount_pad
    )
    override val messageBinding: LayoutConstraintMessageBinding by viewBinding(
        LayoutConstraintMessageBinding::bind, R.id.include_constraint_message
    )
    override val confirmationBinding: LayoutConstraintConfirmButtonBinding by viewBinding(
        LayoutConstraintConfirmButtonBinding::bind, R.id.include_constraint_confirm_button
    )

    override val headerStringId: Int
        get() = R.string.payment_send_header_name

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        confirmationBinding.buttonConfirm.setOnClickListener {
            viewModel.sendPayment(messageBinding.editTextMessage.text?.toString())
        }
    }

    override fun setupFooter() {
        val insetterActivity = (requireActivity() as InsetterActivity)
        insetterActivity.addNavigationBarPadding(binding.layoutConstraintPaymentSend)
    }

    override suspend fun onViewStateFlowCollect(viewState: PaymentSendViewState) {
        @Exhaustive
        when (viewState) {
            is PaymentSendViewState.Idle -> {}

            is PaymentSendViewState.ChatPayment -> {
                binding.includeConstraintFromContact.root.visible
                binding.includeConstraintMessage.root.visible
                binding.includeConstraintConfirmButton.buttonConfirm.text = getString(R.string.confirm_button)

                setupDestination(contact = viewState.contact)
            }

            is PaymentSendViewState.TribePayment -> {
                binding.includeConstraintFromContact.root.visible
                binding.includeConstraintMessage.root.visible
                binding.includeConstraintConfirmButton.buttonConfirm.text = getString(R.string.confirm_button)

                setupDestination(
                    memberAlias = viewState.memberAlias,
                    memberColorKey = viewState.memberColorKey,
                    memberPic = viewState.memberPic
                )
            }

            is PaymentSendViewState.KeySendPayment -> {
                binding.includeConstraintFromContact.root.invisible
                binding.includeConstraintMessage.root.invisible
                binding.includeConstraintConfirmButton.buttonConfirm.text = getString(R.string.continue_button)
            }

            is PaymentSendViewState.ProcessingPayment -> {
                binding.includeConstraintConfirmButton.buttonConfirm.isEnabled = false
                binding.includeConstraintConfirmButton.confirmProgress.visible
            }

            is PaymentSendViewState.PaymentFailed -> {
                binding.includeConstraintConfirmButton.buttonConfirm.isEnabled = true
                binding.includeConstraintConfirmButton.confirmProgress.gone
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: PaymentSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
