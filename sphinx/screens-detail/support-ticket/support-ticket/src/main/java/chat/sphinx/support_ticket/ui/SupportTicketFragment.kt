package chat.sphinx.support_ticket.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.support_ticket.R
import chat.sphinx.support_ticket.databinding.*
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive

@AndroidEntryPoint
internal class SupportTicketFragment: SideEffectFragment<
        Context,
        SupportTicketSideEffect,
        SupportTicketViewState,
        SupportTicketViewModel,
        FragmentSupportTicketBinding
        >(R.layout.fragment_support_ticket)
{
    override val viewModel: SupportTicketViewModel by viewModels()
    override val binding: FragmentSupportTicketBinding by viewBinding(FragmentSupportTicketBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeSupportTicketHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.support_ticket_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupFragmentLayout()
        setupSupportTicketFunctionality()
    }

    private fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity).addNavigationBarPadding(
            binding.includeSupportTicketLayout.layoutConstraintSupportTicket
        )
    }

    private fun setupSupportTicketFunctionality() {
        binding.includeSupportTicketLayout.apply {

            includeButtonCopyLogs.root.setOnClickListener {
                viewModel.loadedLogs()?.let { logs ->
                    val clipboard: ClipboardManager = binding.root.context.getSystemService(
                        ClipboardManager::class.java
                    )
                    val clip = ClipData.newPlainText("Copied Logs", logs)
                    clipboard.setPrimaryClip(clip)
                    viewModel.showLogsCopiedToast()
                }
            }

            includeButtonSendMessage.root.setOnClickListener {
                viewModel.onSendMessage(supportTicketMessageText.text)?.let {
                    startActivity(it)
                }
            }
        }

        viewModel.loadLogs()
    }

    override suspend fun onViewStateFlowCollect(viewState: SupportTicketViewState) {
        binding.includeSupportTicketLayout.apply {
            @Exhaustive
            when(viewState) {
                is SupportTicketViewState.Empty -> {
                    includeLogText.logsTextView.text = ""
                    progressBarLoadingLogs.gone
                }
                is SupportTicketViewState.Fetched -> {
                    includeLogText.logsTextView.text = viewState.logs
                    progressBarLoadingLogs.gone
                }
                SupportTicketViewState.LoadingLogs -> {
                    progressBarLoadingLogs.visible
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: SupportTicketSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
