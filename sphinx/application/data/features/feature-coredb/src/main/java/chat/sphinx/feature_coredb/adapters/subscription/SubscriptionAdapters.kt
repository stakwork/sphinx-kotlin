package chat.sphinx.feature_coredb.adapters.subscription

import chat.sphinx.wrapper_common.subscription.Cron
import chat.sphinx.wrapper_common.subscription.EndNumber
import chat.sphinx.wrapper_common.subscription.SubscriptionCount
import com.squareup.sqldelight.ColumnAdapter

internal class CronAdapter: ColumnAdapter<Cron, String> {
    override fun decode(databaseValue: String): Cron {
        return Cron(databaseValue)
    }

    override fun encode(value: Cron): String {
        return value.value
    }
}

internal class EndNumberAdapter: ColumnAdapter<EndNumber, Long> {
    override fun decode(databaseValue: Long): EndNumber {
        return EndNumber(databaseValue)
    }

    override fun encode(value: EndNumber): Long {
        return value.value
    }
}

internal class SubscriptionCountAdapter: ColumnAdapter<SubscriptionCount, Long> {
    override fun decode(databaseValue: Long): SubscriptionCount {
        return SubscriptionCount(databaseValue)
    }

    override fun encode(value: SubscriptionCount): Long {
        return value.value
    }
}