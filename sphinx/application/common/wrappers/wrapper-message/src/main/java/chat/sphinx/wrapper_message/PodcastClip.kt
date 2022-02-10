package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPodcastClipOrNull(moshi: Moshi): PodcastClip? =
    try {
        this.toPodcastClip(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toPodcastClip(moshi: Moshi): PodcastClip =
    moshi.adapter(PodcastClipMoshi::class.java)
        .fromJson(this)
        ?.let {
            PodcastClip(
                it.text,
                it.title,
                LightningNodePubKey(it.pubkey),
                it.url,
                FeedId(it.feedID),
                FeedId(it.itemID),
                it.ts
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun PodcastClip.toJson(moshi: Moshi): String =
    moshi.adapter(PodcastClipMoshi::class.java)
        .toJson(
            PodcastClipMoshi(
                text ?: "",
                title,
                pubkey.value,
                url,
                feedID.value,
                itemID.value,
                ts
            )
        )

data class PodcastClip(
    val text: String?,
    val title: String,
    val pubkey: LightningNodePubKey,
    val url: String,
    val feedID: FeedId,
    val itemID: FeedId,
    val ts: Int,
) {

    companion object {
        const val MESSAGE_PREFIX = "clip::"
    }
}

@JsonClass(generateAdapter = true)
internal data class PodcastClipMoshi(
    val text: String,
    val title: String,
    val pubkey: String,
    val url: String,
    val feedID: String,
    val itemID: String,
    val ts: Int,
)
