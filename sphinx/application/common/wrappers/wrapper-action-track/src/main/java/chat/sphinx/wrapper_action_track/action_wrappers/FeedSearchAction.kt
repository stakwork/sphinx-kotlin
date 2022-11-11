package chat.sphinx.wrapper_action_track.action_wrappers

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

data class FeedSearchAction(
    val frequency: Long,
    val searchTerm: String,
    val currentTimestamp: Long
)

@JsonClass(generateAdapter = true)
internal data class FeedSearchActionMoshi(
    val frequency: Long,
    val searchTerm: String,
    val currentTimestamp: Long
)

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedSearchActionOrNull(moshi: Moshi): FeedSearchAction? =
    try {
        this.toFeedSearchAction(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toFeedSearchAction(moshi: Moshi): FeedSearchAction =
    moshi.adapter(FeedSearchActionMoshi::class.java)
        .fromJson(this)
        ?.let {
            FeedSearchAction(
                it.frequency,
                it.searchTerm,
                it.currentTimestamp
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun FeedSearchAction.toJson(moshi: Moshi): String =
    moshi.adapter(FeedSearchActionMoshi::class.java)
        .toJson(
            FeedSearchActionMoshi(
                frequency,
                searchTerm,
                currentTimestamp
            )
        )