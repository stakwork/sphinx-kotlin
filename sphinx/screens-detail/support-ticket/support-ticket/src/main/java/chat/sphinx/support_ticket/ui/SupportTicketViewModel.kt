package chat.sphinx.support_ticket.ui

import chat.sphinx.support_ticket.navigation.SupportTicketNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class SupportTicketViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: SupportTicketNavigator,
): BaseViewModel<SupportTicketViewState>(dispatchers, SupportTicketViewState.Idle)
{
}
