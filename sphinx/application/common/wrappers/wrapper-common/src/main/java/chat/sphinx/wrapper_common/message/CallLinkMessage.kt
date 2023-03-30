package chat.sphinx.wrapper_common.message

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

@Suppress("NOTHING_TO_INLINE")
inline fun String.toCallLinkMessageOrNull(moshi: Moshi): CallLinkMessage? =
    try {
        this.toCallLinkMessage(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toCallLinkMessage(moshi: Moshi): CallLinkMessage =
    moshi.adapter(CallLinkMessageMoshi::class.java)
        .fromJson(this)
        ?.let {
            CallLinkMessage(
                SphinxCallLink(it.link),
                it.recurring,
                it.cron
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun CallLinkMessage.toJson(moshi: Moshi): String =
    moshi.adapter(CallLinkMessageMoshi::class.java)
        .toJson(
            CallLinkMessageMoshi(
                link.value,
                recurring,
                cron
            )
        )

data class CallLinkMessage(
    val link: SphinxCallLink,
    val recurring: Boolean,
    val cron: String,
) {
    companion object {
        const val MESSAGE_PREFIX = "call::"
    }
}

@JsonClass(generateAdapter = true)
internal data class CallLinkMessageMoshi(
    val link: String,
    val recurring: Boolean,
    val cron: String
)
