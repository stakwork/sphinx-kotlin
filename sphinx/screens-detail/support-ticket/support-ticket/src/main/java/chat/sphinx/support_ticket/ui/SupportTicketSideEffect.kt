package chat.sphinx.support_ticket.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.support_ticket.R
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class  SupportTicketSideEffect: SideEffect<Context>()  {
    object MessageRequired: SupportTicketSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.message_required)
        }
    }

}