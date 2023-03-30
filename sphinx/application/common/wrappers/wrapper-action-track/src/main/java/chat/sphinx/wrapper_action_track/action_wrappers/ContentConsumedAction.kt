package chat.sphinx.wrapper_action_track.action_wrappers

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

data class ContentConsumedAction(
    val feedId: String,
    val feedType: Long,
    val feedUrl: String,
    val feedItemId: String,
    val feedItemUrl: String,
    val showTitle: String,
    val episodeTitle: String,
    val description: String,
    val clipRank: Long,
    val topics: ArrayList<String>,
    val people: ArrayList<String>,
    val publishedDate: Long,
) {
    var history: ArrayList<ContentConsumedHistoryItem> = arrayListOf()

    fun addHistoryItem(item: ContentConsumedHistoryItem) {
        history.add(item)
    }
}

@JsonClass(generateAdapter = true)
internal data class ContentConsumedActionMoshi(
    val feedId: String,
    val feedType: Long,
    val feedUrl: String,
    val feedItemId: String,
    val feedItemUrl: String,
    val showTitle: String,
    val episodeTitle: String,
    val description: String,
    val clipRank: Long,
    val people: List<String>,
    val publishedDate: Long,
    val history: List<ContentConsumedHistoryItemMoshi>
)

@Suppress("NOTHING_TO_INLINE")
inline fun String.toContentConsumedActionOrNull(moshi: Moshi): ContentConsumedAction? =
    try {
        this.toContentConsumedAction(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toContentConsumedAction(moshi: Moshi): ContentConsumedAction {
    val contentConsumedAction = moshi.adapter(ContentConsumedActionMoshi::class.java)
        .fromJson(this)
        ?.let {
            var action = ContentConsumedAction(
                it.feedId,
                it.feedType,
                it.feedUrl,
                it.feedItemId,
                it.feedItemUrl,
                it.showTitle,
                it.episodeTitle,
                it.description,
                it.clipRank,
                arrayListOf(),
                ArrayList(it.people),
                it.publishedDate
            )

            val history: ArrayList<ContentConsumedHistoryItem> = arrayListOf()

            for (item in it.history) {
                history.add(
                    ContentConsumedHistoryItem(
                        ArrayList(item.topics),
                        item.startTimestamp,
                        item.endTimestamp,
                        item.currentTimestamp
                    )
                )
            }

            action.history = history
            action
        }

    return contentConsumedAction ?: throw IllegalArgumentException("Provided Json was invalid")

}


@Throws(AssertionError::class)
fun ContentConsumedAction.toJson(moshi: Moshi): String {
    val history: MutableList<ContentConsumedHistoryItemMoshi> = mutableListOf()

    for (item in this.history) {
        history.add(
            ContentConsumedHistoryItemMoshi(
                topics,
                item.startTimestamp,
                item.endTimestamp,
                item.currentTimestamp
            )
        )
    }

    return moshi.adapter(ContentConsumedActionMoshi::class.java)
        .toJson(
            ContentConsumedActionMoshi(
                feedId,
                feedType,
                feedUrl,
                feedItemId,
                feedItemUrl,
                showTitle,
                episodeTitle,
                description,
                clipRank,
                people,
                publishedDate,
                history
            )
        )
}


data class ContentConsumedHistoryItem(
    val topics: ArrayList<String>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val currentTimestamp: Long
)

@JsonClass(generateAdapter = true)
internal data class ContentConsumedHistoryItemMoshi(
    val topics: List<String>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val currentTimestamp: Long
)

@Suppress("NOTHING_TO_INLINE")
inline fun String.toContentConsumedHistoryItemOrNull(moshi: Moshi): ContentConsumedHistoryItem? =
    try {
        this.toContentConsumedHistoryItem(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toContentConsumedHistoryItem(moshi: Moshi): ContentConsumedHistoryItem =
    moshi.adapter(ContentConsumedHistoryItemMoshi::class.java)
        .fromJson(this)
        ?.let {
            ContentConsumedHistoryItem(
                ArrayList(it.topics),
                it.startTimestamp,
                it.endTimestamp,
                it.currentTimestamp,
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun ContentConsumedHistoryItem.toJson(moshi: Moshi): String =
    moshi.adapter(ContentConsumedHistoryItemMoshi::class.java)
        .toJson(
            ContentConsumedHistoryItemMoshi(
                topics,
                startTimestamp,
                endTimestamp,
                currentTimestamp,
            )
        )


