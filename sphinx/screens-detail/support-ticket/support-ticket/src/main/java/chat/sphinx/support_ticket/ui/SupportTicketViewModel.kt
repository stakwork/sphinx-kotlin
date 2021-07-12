package chat.sphinx.support_ticket.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.support_ticket.navigation.SupportTicketNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
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
    fun loadLogs(logsTextView: AppCompatTextView) {
        viewModelScope.launch(mainImmediate) {
            networkQueryLightning.getLogs().collect { loadedResponse ->
                @Exhaustive
                when (loadedResponse) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        submitSideEffect(SupportTicketSideEffect.FailedToFetchLogs)
                    }
                    is Response.Success -> {
                        logsTextView.text = loadedResponse.value
                    }
                }
            }
        }
    }

    fun showNoLogsToCopyMessage() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(SupportTicketSideEffect.NoLogsToCopy)
        }
    }

    fun onSendMessage(text: Editable?, logs: CharSequence): Intent?  {
        return if (!text.isNullOrEmpty()) {
            var body = text.toString()
            if (logs.isNotEmpty()) {
                body += "\n\n$logs.toString()"
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
