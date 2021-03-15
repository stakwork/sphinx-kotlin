package chat.sphinx.concept_network_query_subscription.model

import chat.sphinx.concept_network_query_chat.model.ChatDto

@Suppress("PropertyName")
abstract class BaseSubscriptionDto<Ended, Paused> {
    abstract val id: Long
    abstract val chat_id: Long
    abstract val contact_id: Long
    abstract val cron: String
    abstract val amount: Long
    abstract val total_paid: Long
    abstract val end_number: Int?
    abstract val end_date: String?
    abstract val count: Int

    // from '/contacts' endpoint, this comes in as an Int while the
    // '/subscriptions' endpoint it is a boolean
    abstract val ended: Ended

    // from '/contacts' endpoint, this comes in as an Int while the
    // '/subscriptions' endpoint it is a boolean
    abstract val paused: Paused

    abstract val created_at: String
    abstract val updated_at: String
    abstract val tenant: Int
    abstract val interval: String
    abstract val next: String
    abstract val chat: ChatDto?
}
