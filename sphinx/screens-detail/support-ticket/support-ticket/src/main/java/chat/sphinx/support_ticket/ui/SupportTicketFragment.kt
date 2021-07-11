package chat.sphinx.support_ticket.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.support_ticket.R
import chat.sphinx.support_ticket.databinding.FragmentSupportTicketBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import kotlinx.coroutines.launch

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
        val activity = (requireActivity() as InsetterActivity)
        binding.apply {
            activity.addNavigationBarPadding(
                includeSupportTicketLayout.layoutConstraintSupportTicket
            )
        }
    }

    private fun setupSupportTicketFunctionality() {
        binding.apply {
            includeSupportTicketLayout.layoutButtonSendMessage.layoutConstraintButtonSendMessage.setOnClickListener {
                viewModel.onSendMessage(includeSupportTicketLayout.supportTicketMessageText.text)?.let {
                    startActivity(it)
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: SupportTicketViewState) {
//        TODO("Not yet implemented")
    }

    override suspend fun onSideEffectCollect(sideEffect: SupportTicketSideEffect) {
        sideEffect.execute(requireActivity())
    }
}
