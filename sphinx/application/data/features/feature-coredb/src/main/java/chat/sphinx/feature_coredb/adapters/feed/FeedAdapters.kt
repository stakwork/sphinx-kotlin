package chat.sphinx.feature_coredb.adapters.feed

import chat.sphinx.wrapper_common.feed.*
import chat.sphinx.wrapper_feed.*
import com.squareup.sqldelight.ColumnAdapter

internal class FeedIdAdapter: ColumnAdapter<FeedId, String> {

    override fun decode(databaseValue: String): FeedId {
        return FeedId(databaseValue)
    }

    override fun encode(value: FeedId): String {
        return value.value
    }
}

internal class FeedTypeAdapter: ColumnAdapter<FeedType, Long> {

    override fun decode(databaseValue: Long): FeedType {
        return databaseValue.toInt().toFeedType()
    }

    override fun encode(value: FeedType): Long {
        return value.value.toLong()
    }
}

internal class FeedTitleAdapter: ColumnAdapter<FeedTitle, String> {

    override fun decode(databaseValue: String): FeedTitle {
        return FeedTitle(databaseValue)
    }

    override fun encode(value: FeedTitle): String {
        return value.value
    }
}

internal class FeedDescriptionAdapter: ColumnAdapter<FeedDescription, String> {

    override fun decode(databaseValue: String): FeedDescription {
        return FeedDescription(databaseValue)
    }

    override fun encode(value: FeedDescription): String {
        return value.value
    }
}

internal class FeedAuthorAdapter: ColumnAdapter<FeedAuthor, String> {

    override fun decode(databaseValue: String): FeedAuthor {
        return FeedAuthor(databaseValue)
    }

    override fun encode(value: FeedAuthor): String {
        return value.value
    }
}

internal class FeedGeneratorAdapter: ColumnAdapter<FeedGenerator, String> {

    override fun decode(databaseValue: String): FeedGenerator {
        return FeedGenerator(databaseValue)
    }

    override fun encode(value: FeedGenerator): String {
        return value.value
    }
}

internal class FeedUrlAdapter private constructor(): ColumnAdapter<FeedUrl, String> {

    companion object {
        @Volatile
        private var instance: FeedUrlAdapter? = null
        fun getInstance(): FeedUrlAdapter =
            instance ?: synchronized(this) {
                instance ?: FeedUrlAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): FeedUrl {
        return FeedUrl(databaseValue)
    }

    override fun encode(value: FeedUrl): String {
        return value.value
    }
}

internal class FeedContentTypeAdapter: ColumnAdapter<FeedContentType, String> {

    override fun decode(databaseValue: String): FeedContentType {
        return FeedContentType(databaseValue)
    }

    override fun encode(value: FeedContentType): String {
        return value.value
    }
}

internal class FeedLanguageAdapter: ColumnAdapter<FeedLanguage, String> {

    override fun decode(databaseValue: String): FeedLanguage {
        return FeedLanguage(databaseValue)
    }

    override fun encode(value: FeedLanguage): String {
        return value.value
    }
}

internal class FeedItemsCountAdapter: ColumnAdapter<FeedItemsCount, Long> {

    override fun decode(databaseValue: Long): FeedItemsCount {
        return FeedItemsCount(databaseValue)
    }

    override fun encode(value: FeedItemsCount): Long {
        return value.value
    }
}

internal class FeedEnclosureLengthAdapter: ColumnAdapter<FeedEnclosureLength, Long> {

    override fun decode(databaseValue: Long): FeedEnclosureLength {
        return FeedEnclosureLength(databaseValue)
    }

    override fun encode(value: FeedEnclosureLength): Long {
        return value.value
    }
}

internal class FeedEnclosureTypeAdapter: ColumnAdapter<FeedEnclosureType, String> {

    override fun decode(databaseValue: String): FeedEnclosureType {
        return FeedEnclosureType(databaseValue)
    }

    override fun encode(value: FeedEnclosureType): String {
        return value.value
    }
}

internal class FeedModelTypeAdapter: ColumnAdapter<FeedModelType, String> {

    override fun decode(databaseValue: String): FeedModelType {
        return FeedModelType(databaseValue)
    }

    override fun encode(value: FeedModelType): String {
        return value.value
    }
}

internal class FeedModelSuggestedAdapter: ColumnAdapter<FeedModelSuggested, Double> {

    override fun decode(databaseValue: Double): FeedModelSuggested {
        return FeedModelSuggested(databaseValue)
    }

    override fun encode(value: FeedModelSuggested): Double {
        return value.value
    }
}

internal class FeedDestinationAddressAdapter: ColumnAdapter<FeedDestinationAddress, String> {

    override fun decode(databaseValue: String): FeedDestinationAddress {
        return FeedDestinationAddress(databaseValue)
    }

    override fun encode(value: FeedDestinationAddress): String {
        return value.value
    }
}

internal class FeedDestinationSplitAdapter: ColumnAdapter<FeedDestinationSplit, Double> {

    override fun decode(databaseValue: Double): FeedDestinationSplit {
        return FeedDestinationSplit(databaseValue)
    }

    override fun encode(value: FeedDestinationSplit): Double {
        return value.value
    }
}

internal class FeedDestinationTypeAdapter: ColumnAdapter<FeedDestinationType, String> {

    override fun decode(databaseValue: String): FeedDestinationType {
        return FeedDestinationType(databaseValue)
    }

    override fun encode(value: FeedDestinationType): String {
        return value.value
    }
}

internal class SubscribedAdapter private constructor(): ColumnAdapter<Subscribed, Long> {

    companion object {
        @Volatile
        private var instance: SubscribedAdapter? = null
        fun getInstance(): SubscribedAdapter =
            instance ?: synchronized(this) {
                instance ?: SubscribedAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): Subscribed {
        return databaseValue.toInt().toSubscribed()
    }

    override fun encode(value: Subscribed): Long {
        return value.value.toLong()
    }
}

internal class FeedItemDurationAdapter: ColumnAdapter<FeedItemDuration, Long> {

    override fun decode(databaseValue: Long): FeedItemDuration {
        return FeedItemDuration(databaseValue)
    }

    override fun encode(value: FeedItemDuration): Long {
        return value.value
    }
}

internal class PlayerSpeedAdapter: ColumnAdapter<FeedPlayerSpeed, Double> {

    override fun decode(databaseValue: Double): FeedPlayerSpeed {
        return FeedPlayerSpeed(databaseValue)
    }

    override fun encode(value: FeedPlayerSpeed): Double {
        return value.value
    }
}