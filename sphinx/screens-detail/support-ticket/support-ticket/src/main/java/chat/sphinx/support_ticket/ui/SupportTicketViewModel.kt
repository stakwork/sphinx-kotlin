package chat.sphinx.support_ticket.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class SupportTicketViewModel @Inject constructor(

): BaseViewModel<SupportTicketViewState>(SupportTicketViewState.Idle)
{
}
