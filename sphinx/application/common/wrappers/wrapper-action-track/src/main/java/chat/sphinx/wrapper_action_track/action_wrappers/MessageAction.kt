package chat.sphinx.wrapper_action_track.action_wrappers

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

data class MessageAction(
    val keywords: ArrayList<String>,
    val currentTimestamp: Long
)

@JsonClass(generateAdapter = true)
internal data class MessageActionMoshi(
    val keywords: List<String>,
    val currentTimestamp: Long
)

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMessageActionOrNull(moshi: Moshi): MessageAction? =
    try {
        this.toMessageAction(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toMessageAction(moshi: Moshi): MessageAction =
    moshi.adapter(MessageActionMoshi::class.java)
        .fromJson(this)
        ?.let {
            MessageAction(
                ArrayList(it.keywords),
                it.currentTimestamp
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun MessageAction.toJson(moshi: Moshi): String =
    moshi.adapter(MessageActionMoshi::class.java)
        .toJson(
            MessageActionMoshi(
                keywords,
                currentTimestamp
            )
        )