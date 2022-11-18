package chat.sphinx.wrapper_action_track.action_wrappers

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

data class PodcastClipCommentAction(
    val feedId: String,
    val feedType: Long,
    val feedUrl: String,
    val feedItemId: String,
    val feedItemUrl: String,
    val topics: ArrayList<String>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val currentTimestamp: Long
)

@JsonClass(generateAdapter = true)
internal data class PodcastClipCommentActionMoshi(
    val feedId: String,
    val feedType: Long,
    val feedUrl: String,
    val feedItemId: String,
    val feedItemUrl: String,
    val topics: List<String>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val currentTimestamp: Long
)

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPodcastClipCommentActionOrNull(moshi: Moshi): PodcastClipCommentAction? =
    try {
        this.toPodcastClipCommentAction(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toPodcastClipCommentAction(moshi: Moshi): PodcastClipCommentAction =
    moshi.adapter(PodcastClipCommentActionMoshi::class.java)
        .fromJson(this)
        ?.let {
            PodcastClipCommentAction(
                it.feedId,
                it.feedType,
                it.feedUrl,
                it.feedItemId,
                it.feedItemUrl,
                ArrayList(it.topics),
                it.startTimestamp,
                it.endTimestamp,
                it.currentTimestamp
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun PodcastClipCommentAction.toJson(moshi: Moshi): String =
    moshi.adapter(PodcastClipCommentActionMoshi::class.java)
        .toJson(
            PodcastClipCommentActionMoshi(
                feedId,
                feedType,
                feedUrl,
                feedItemId,
                feedItemUrl,
                topics,
                startTimestamp,
                endTimestamp,
                currentTimestamp
            )
        )