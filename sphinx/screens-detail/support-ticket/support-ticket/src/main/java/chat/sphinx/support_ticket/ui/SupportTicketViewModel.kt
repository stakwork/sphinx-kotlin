package chat.sphinx.support_ticket.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.support_ticket.navigation.SupportTicketNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class SupportTicketViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: SupportTicketNavigator,
    private val actionsRepository: ActionsRepository,
    private val networkQueryLightning: NetworkQueryLightning,
): SideEffectViewModel<
        Context,
        SupportTicketSideEffect,
        SupportTicketViewState>(dispatchers, SupportTicketViewState.Empty)
{

    fun loadLogs() {
        viewModelScope.launch(mainImmediate) {
            networkQueryLightning.getLogs().collect { loadedResponse ->
                @Exhaustive
                when (loadedResponse) {
                    is LoadResponse.Loading -> {
                        updateViewState(SupportTicketViewState.LoadingLogs)
                    }
                    is Response.Error -> {
                        submitSideEffect(SupportTicketSideEffect.FailedToFetchLogs)
                        updateViewState(SupportTicketViewState.Empty)
                    }
                    is Response.Success -> {
                        updateViewState(
                            SupportTicketViewState.Fetched(
                                loadedResponse.value,
                                actionsRepository.appLogsStateFlow.value
                            )
                        )
                    }
                }
            }
        }
    }

    fun showLogsCopiedToast() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(SupportTicketSideEffect.LogsCopiedToClipboard)
        }
    }

    fun loadedLogs(): String? {
        currentViewState.let { logsViewState ->
            return when (logsViewState) {
                is SupportTicketViewState.Empty -> {
                    viewModelScope.launch(mainImmediate) {
                        submitSideEffect(SupportTicketSideEffect.NoLogsLoaded)
                    }
                    null
                }
                is SupportTicketViewState.Fetched -> {
                    "${logsViewState.logs}" +
                    "\n\n\n" +
                    "${actionsRepository.appLogsStateFlow.value}"
                }
                is SupportTicketViewState.LoadingLogs -> {
                    null
                }
            }
        }
    }

    fun onSendMessage(text: Editable?): Intent?  {
        currentViewState.let { logsViewState ->
            return if (!text.isNullOrEmpty()) {
                val body = when(logsViewState) {
                    is SupportTicketViewState.Empty -> {
                        text
                    }
                    is SupportTicketViewState.LoadingLogs -> {
                        text
                    }
                    is SupportTicketViewState.Fetched -> {
                        "$text\n\n\n${logsViewState.logs}"
                    }
                }

                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@stakwork.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Sphinx Android Support")
                    putExtra(Intent.EXTRA_TEXT, body)
                }
            } else {
                viewModelScope.launch(mainImmediate) {
                    submitSideEffect(SupportTicketSideEffect.MessageRequired)
                }
                null
            }
        }
    }
}
