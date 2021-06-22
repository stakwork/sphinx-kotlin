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
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch

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

        binding.textViewCancelButton.setOnClickListener {
            viewModel.processResponse(SendAttachmentResponse(0))
        }
    }

    private fun setupFooter() {
        val insetterActivity = (requireActivity() as InsetterActivity)
        insetterActivity.addNavigationBarPadding(binding.layoutConstraintMenuContainer)
    }

    override suspend fun onViewStateFlowCollect(viewState: SendAttachmentViewState) {
//        TODO("Not yet implemented")
    }
}