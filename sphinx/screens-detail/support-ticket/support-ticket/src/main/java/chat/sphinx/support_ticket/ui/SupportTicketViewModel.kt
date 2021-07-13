package chat.sphinx.support_ticket.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.support_ticket.navigation.SupportTicketNavigator
import chat.sphinx.support_ticket.ui.viewstate.LogsViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@HiltViewModel
internal class SupportTicketViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: SupportTicketNavigator,
    private val networkQueryLightning: NetworkQueryLightning,
): SideEffectViewModel<
        Context,
        SupportTicketSideEffect,
        SupportTicketViewState>(dispatchers, SupportTicketViewState.Idle)
{
    private inner class LogsStateContainer: ViewStateContainer<LogsViewState>(LogsViewState.Empty)

    private val logsStateContainer: ViewStateContainer<LogsViewState> by lazy {
        LogsStateContainer()
    }

    @JvmSynthetic
    internal fun getLogsViewStateFlow(): StateFlow<LogsViewState> =
        logsStateContainer.viewStateFlow

    @JvmSynthetic
    internal fun updateLogsSendViewState(viewState: LogsViewState) {
        logsStateContainer.updateViewState(viewState)
    }

    fun loadLogs() {
        viewModelScope.launch(mainImmediate) {
            networkQueryLightning.getLogs().collect { loadedResponse ->
                @Exhaustive
                when (loadedResponse) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        submitSideEffect(SupportTicketSideEffect.FailedToFetchLogs)
                        updateLogsSendViewState(LogsViewState.Empty)
                    }
                    is Response.Success -> {
                        updateLogsSendViewState(LogsViewState.Fetched(loadedResponse.value))
                    }
                }
            }
        }
    }

    fun loadedLogs(): String? {
        logsStateContainer.value.let { logsViewState ->
            return when (logsViewState) {
                is LogsViewState.Empty -> {
                    viewModelScope.launch(mainImmediate) {
                        submitSideEffect(SupportTicketSideEffect.NoLogsToCopy)
                    }
                    null
                }
                is LogsViewState.Fetched -> {
                    logsViewState.logs
                }
            }
        }
    }

    fun onSendMessage(text: Editable?): Intent?  {
        logsStateContainer.value.let { logsViewState ->
            return if (!text.isNullOrEmpty()) {
                val body = when(logsViewState) {
                    LogsViewState.Empty -> {
                        text
                    }
                    is LogsViewState.Fetched -> {
                        "$text\n\n${logsViewState.logs}"
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
