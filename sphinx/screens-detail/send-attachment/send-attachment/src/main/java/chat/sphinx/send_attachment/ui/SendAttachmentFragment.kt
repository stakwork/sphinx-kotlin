package chat.sphinx.send_attachment.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.send_attachment.R
import chat.sphinx.send_attachment.databinding.FragmentSendAttachmentBinding
import chat.sphinx.send_attachment_view_model_coordinator.response.SendAttachmentResponse
import chat.sphinx.wrapper_chat.ChatActionType
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class SendAttachmentFragment: BaseFragment<
        SendAttachmentViewState,
        SendAttachmentViewModel,
        FragmentSendAttachmentBinding
        >(R.layout.fragment_send_attachment)
{
    override val viewModel: SendAttachmentViewModel by viewModels()
    override val binding: FragmentSendAttachmentBinding by viewBinding(FragmentSendAttachmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFooter()
        setOnClickListeners()
    }

    private fun setupFooter() {
        val insetterActivity = (requireActivity() as InsetterActivity)
        insetterActivity.addNavigationBarPadding(binding.layoutConstraintMenuContainer)
    }

    private fun setOnClickListeners() {
        binding.apply {
            textViewCancelButton.setOnClickListener {
                viewModel.processResponse(SendAttachmentResponse(ChatActionType.CancelAction))
            }

            layoutConstraintCameraOptionContainer.setOnClickListener {
                viewModel.processResponse(SendAttachmentResponse(ChatActionType.OpenCamera))
            }

            layoutConstraintLibraryOptionContainer.setOnClickListener {
                viewModel.processResponse(SendAttachmentResponse(ChatActionType.OpenPhotoLibrary))
            }

            layoutConstraintGifOptionContainer.setOnClickListener {
                viewModel.processResponse(SendAttachmentResponse(ChatActionType.OpenGifSearch))
            }

            layoutConstraintFileOptionContainer.setOnClickListener {
                viewModel.processResponse(SendAttachmentResponse(ChatActionType.OpenFileLibrary))
            }

            layoutConstraintPaidMessageOptionContainer.setOnClickListener {
                viewModel.processResponse(SendAttachmentResponse(ChatActionType.OpenPaidMessageScreen))
            }

            layoutConstraintRequestOptionContainer.setOnClickListener {
                viewModel.processResponse(SendAttachmentResponse(ChatActionType.RequestAmount))
            }

            layoutConstraintSendOptionContainer.setOnClickListener {
                viewModel.processResponse(SendAttachmentResponse(ChatActionType.SendPayment))
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: SendAttachmentViewState) {
        binding.apply {
            @Exhaustive
            when (viewState) {
                is SendAttachmentViewState.LayoutVisibility -> {
                    val alpha = if(viewState.paymentAndInvoiceEnabled) 1.0F else 0.4F
                    layoutConstraintRequestOptionContainer.alpha = alpha
                    layoutConstraintRequestOptionContainer.isEnabled = viewState.paymentAndInvoiceEnabled

                    layoutConstraintSendOptionContainer.alpha = alpha
                    layoutConstraintSendOptionContainer.isEnabled = viewState.paymentAndInvoiceEnabled
                }
            }
        }
    }
}
