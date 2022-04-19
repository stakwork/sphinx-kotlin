package chat.sphinx.payment_receive.ui

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
import chat.sphinx.payment_common.ui.viewstate.receive.PaymentReceiveViewState
import chat.sphinx.payment_receive.R
import chat.sphinx.payment_receive.databinding.FragmentPaymentReceiveBinding
import chat.sphinx.resources.databinding.LayoutAmountPadBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class PaymentReceiveFragment: PaymentFragment<
        FragmentPaymentReceiveBinding,
        PaymentReceiveFragmentArgs,
        PaymentReceiveViewModel,
        PaymentReceiveViewState
        >(R.layout.fragment_payment_receive)

{
    override val viewModel: PaymentReceiveViewModel by viewModels()
    override val binding: FragmentPaymentReceiveBinding by viewBinding(FragmentPaymentReceiveBinding::bind)

    override val headerBinding: LayoutDetailScreenHeaderBinding by viewBinding(
        LayoutDetailScreenHeaderBinding::bind, R.id.include_payment_receive_header
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
        get() = R.string.payment_receive_header_name

    @Inject
    protected lateinit var imageLoaderInj: ImageLoader<ImageView>
    override val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        confirmationBinding.buttonConfirm.setOnClickListener {
            viewModel.requestPayment(messageBinding.editTextMessage.text?.toString())
        }
    }

    override fun setupFooter() {
        val insetterActivity = (requireActivity() as InsetterActivity)
        insetterActivity.addNavigationBarPadding(binding.layoutConstraintPaymentReceive)
    }

    override suspend fun onViewStateFlowCollect(viewState: PaymentReceiveViewState) {
        @Exhaustive
        when (viewState) {
            is PaymentReceiveViewState.Idle -> {}

            is PaymentReceiveViewState.ChatPaymentRequest -> {
                binding.includeConstraintFromContact.root.visible
                binding.includeConstraintMessage.root.visible
                binding.includeConstraintConfirmButton.buttonConfirm.text = getString(R.string.confirm_button)

                setupDestination(contact = viewState.contact)
            }

            is PaymentReceiveViewState.RequestLightningPayment -> {
                binding.includeConstraintFromContact.root.invisible
                binding.includeConstraintMessage.root.visible
                binding.includeConstraintConfirmButton.buttonConfirm.text = getString(R.string.continue_button)
            }

            is PaymentReceiveViewState.ProcessingRequest -> {
                binding.includeConstraintConfirmButton.buttonConfirm.isEnabled = false
                binding.includeConstraintConfirmButton.confirmProgress.visible
            }

            is PaymentReceiveViewState.RequestFailed -> {
                binding.includeConstraintConfirmButton.buttonConfirm.isEnabled = true
                binding.includeConstraintConfirmButton.confirmProgress.gone
            }
        }
    }

}
