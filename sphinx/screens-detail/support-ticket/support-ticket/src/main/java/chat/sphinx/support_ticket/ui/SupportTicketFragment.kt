package chat.sphinx.support_ticket.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.detail_resources.databinding.LayoutDetailScreenHeaderBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.support_ticket.R
import chat.sphinx.support_ticket.databinding.*
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.flow.collect
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

    private val includeSupportTicketHeader: LayoutDetailScreenHeaderBinding by viewBinding(
        LayoutDetailScreenHeaderBinding::bind, R.id.include_support_ticket_header
    )
    private val includeSupportTicketLayout: LayoutSupportTicketBinding by viewBinding(
        LayoutSupportTicketBinding::bind, R.id.include_support_ticket_layout
    )
    private val sendMessageButton: LayoutButtonSendMessageBinding by viewBinding(
        LayoutButtonSendMessageBinding::bind, R.id.include_button_send_message
    )
    private val copyLogsButton: LayoutButtonCopyLogsBinding by viewBinding(
        LayoutButtonCopyLogsBinding::bind, R.id.include_button_copy_logs
    )
    private val layoutLogsTextBinding: LayoutLogsTextBinding by viewBinding(
        LayoutLogsTextBinding::bind, R.id.include_log_text
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        includeSupportTicketHeader.apply {
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
        val activity = (requireActivity() as InsetterActivity)
        activity.addNavigationBarPadding(
            includeSupportTicketLayout.layoutConstraintSupportTicket
        )
    }

    private fun setupSupportTicketFunctionality() {
        copyLogsButton.root.setOnClickListener {
            viewModel.loadedLogs()?.let { logs ->
                context?.let {
                    val clipboard: ClipboardManager = it.getSystemService(
                        ClipboardManager::class.java
                    )
                    val clip = ClipData.newPlainText("Copied Logs", logs)
                    clipboard.setPrimaryClip(clip)
                    viewModel.showLogsCopiedToast()
                }
            }
        }

        sendMessageButton.root.setOnClickListener {
            viewModel.onSendMessage(includeSupportTicketLayout.supportTicketMessageText.text)?.let {
                startActivity(it)
            }
        }

        viewModel.loadLogs()
    }

    override suspend fun onViewStateFlowCollect(viewState: SupportTicketViewState) {
//        TODO("Not yet implemented")
    }

    override suspend fun onSideEffectCollect(sideEffect: SupportTicketSideEffect) {
        sideEffect.execute(requireActivity())
    }

    override fun onStart() {
        super.onStart()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getLogsViewStateFlow().collect { viewState ->
                layoutLogsTextBinding.apply {
                    @Exhaustive
                    when(viewState) {
                        is SupportTicketViewState.Empty -> {
                            logsTextView.text = ""
                            includeSupportTicketLayout.progressBarLoadingLogs.gone
                        }
                        is SupportTicketViewState.Fetched -> {
                            logsTextView.text = viewState.logs
                            includeSupportTicketLayout.progressBarLoadingLogs.gone
                        }
                        SupportTicketViewState.LoadingLogs -> {
                            includeSupportTicketLayout.progressBarLoadingLogs.visible
                        }
                    }
                }
            }
        }
    }
}
