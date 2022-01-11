package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.feed.FeedId
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPodBoostOrNull(moshi: Moshi): FeedBoost? =
    try {
        this.toPodBoost(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toPodBoost(moshi: Moshi): FeedBoost =
    moshi.adapter(PodBoostMoshi::class.java)
        .fromJson(this)
        ?.let {
            FeedBoost(
                FeedId(it.feedID),
                FeedId(it.itemID),
                it.ts,
                Sat(it.amount)
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun FeedBoost.toJson(moshi: Moshi): String =
    moshi.adapter(PodBoostMoshi::class.java)
        .toJson(
            PodBoostMoshi(
                feedId.value,
                itemId.value,
                timeSeconds,
                amount.value
            )
        )

data class FeedBoost(
    val feedId: FeedId,
    val itemId: FeedId,
    val timeSeconds: Int,
    val amount: Sat
) {
    companion object {
        const val MESSAGE_PREFIX = "boost::"
    }
}

// "{\"feedID\":\"226249\",\"itemID\":\"1997782557\",\"ts\":1396,\"amount\":100}"
@JsonClass(generateAdapter = true)
internal data class PodBoostMoshi(
    val feedID: String,
    val itemID: String,
    val ts: Int,
    val amount: Long
)
