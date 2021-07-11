package chat.sphinx.support_ticket.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import androidx.lifecycle.viewModelScope
import chat.sphinx.support_ticket.navigation.SupportTicketNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SupportTicketViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: SupportTicketNavigator,
): SideEffectViewModel<
        Context,
        SupportTicketSideEffect,
        SupportTicketViewState>(dispatchers, SupportTicketViewState.Idle)
{
    fun onSendMessage(text: Editable?): Intent?  {
        return if (!text.isNullOrEmpty()) {
            val uri = Uri.parse("mailto:support@stakwork.com")
                .buildUpon()
                .appendQueryParameter("subject", "Sphinx Android Support")
                .appendQueryParameter("body", text.toString())
                .build()

            Intent(Intent.ACTION_SENDTO, uri)
        } else {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(SupportTicketSideEffect.MessageRequired)
            }
            null
        }
    }


}
