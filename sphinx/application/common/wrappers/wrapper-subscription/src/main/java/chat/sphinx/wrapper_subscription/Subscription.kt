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
    val endNumber: EndNumber?,
    val count: SubscriptionCount,
    val endDate: DateTime?,
    val ended: Boolean,
    val paused: Boolean,
    val createdAt: DateTime,
    val updatedAt: DateTime,
    val chatId: ChatId,
    val contactId: ContactId
)