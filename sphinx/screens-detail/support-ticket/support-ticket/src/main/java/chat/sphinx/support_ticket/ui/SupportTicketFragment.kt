package chat.sphinx.support_ticket.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.support_ticket.R
import chat.sphinx.support_ticket.databinding.FragmentSupportTicketBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class SupportTicketFragment: BaseFragment<
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
//            textViewDetailScreenClose.setOnClickListener {
            // TODO: Navigate
//            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: SupportTicketViewState) {
//        TODO("Not yet implemented")
    }
}
