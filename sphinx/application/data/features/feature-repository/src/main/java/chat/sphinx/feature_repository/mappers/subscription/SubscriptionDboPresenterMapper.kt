package chat.sphinx.feature_repository.mappers.subscription

import chat.sphinx.conceptcoredb.SubscriptionDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_subscription.Subscription
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

@Suppress("NOTHING_TO_INLINE")
inline fun SubscriptionDbo.toSubscription(): Subscription =
    Subscription(
        id,
        cron,
        amount,
        end_number,
        count,
        end_date,
        ended,
        paused,
        created_at,
        updated_at,
        chat_id,
        contact_id
    )

internal class SubscriptionDboPresenterMapper(
    dispatchers: CoroutineDispatchers
): ClassMapper<SubscriptionDbo, Subscription>(dispatchers) {
    override suspend fun mapFrom(value: SubscriptionDbo): Subscription {
        return value.toSubscription()
    }

    override suspend fun mapTo(value: Subscription): SubscriptionDbo {
        value.apply {
            return SubscriptionDbo(
                id,
                cron,
                amount,
                endNumber,
                count,
                endDate,
                ended,
                paused,
                createdAt,
                updatedAt,
                chatId,
                contactId
            )
        }
    }
}
