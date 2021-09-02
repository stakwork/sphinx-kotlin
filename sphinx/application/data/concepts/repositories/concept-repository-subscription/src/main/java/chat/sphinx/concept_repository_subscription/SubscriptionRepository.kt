package chat.sphinx.concept_repository_subscription

import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.subscription.EndNumber
import chat.sphinx.wrapper_common.subscription.SubscriptionId
import chat.sphinx.wrapper_subscription.Subscription
import kotlinx.coroutines.flow.Flow


interface SubscriptionRepository {
    fun getActiveSubscriptionByContactId(
        contactId: ContactId
    ): Flow<Subscription?>

    suspend fun createSubscription(
        amount: Sat,
        interval: String,
        contactId: ContactId,
        chatId: ChatId?,
        endDate: String?,
        endNumber: EndNumber?
    ): Response<Any, ResponseError>

    suspend fun updateSubscription(
        id: SubscriptionId,
        amount: Sat,
        interval: String,
        contactId: ContactId,
        chatId: ChatId?,
        endDate: String?,
        endNumber: EndNumber?
    ): Response<Any, ResponseError>

    suspend fun restartSubscription(
        subscriptionId: SubscriptionId
    ): Response<Any, ResponseError>

    suspend fun pauseSubscription(
        subscriptionId: SubscriptionId
    ): Response<Any, ResponseError>

    suspend fun deleteSubscription(
        subscriptionId: SubscriptionId
    ): Response<Any, ResponseError>
}
