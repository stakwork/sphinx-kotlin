package chat.sphinx.wrapper_subscription

import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.subscription.Cron
import chat.sphinx.wrapper_common.subscription.EndNumber
import chat.sphinx.wrapper_common.subscription.SubscriptionCount
import chat.sphinx.wrapper_common.subscription.SubscriptionId

data class Subscription(
    val id: SubscriptionId,
    val cron: Cron,
    val amount: Sat,
    val end_number: EndNumber?,
    val count: SubscriptionCount,
    val end_date: DateTime?,
    val ended: Boolean,
    val paused: Boolean,
    val created_at: DateTime,
    val updated_at: DateTime,
    val chat_id: ChatId,
    val contact_id: ContactId
)