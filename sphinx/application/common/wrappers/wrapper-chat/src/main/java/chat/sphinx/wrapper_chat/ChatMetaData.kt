package chat.sphinx.wrapper_chat

import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.lightning.Sat
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

@Suppress("NOTHING_TO_INLINE")
inline fun String.toChatMetaDataOrNull(moshi: Moshi): ChatMetaData? =
    try {
        this.toChatMetaData(moshi)
    } catch (e: Exception) {
        null
    }

@Throws(
    IllegalArgumentException::class,
    JsonDataException::class
)
fun String.toChatMetaData(moshi: Moshi): ChatMetaData =
    moshi.adapter(ChatMetaDataMoshi::class.java)
        .fromJson(this)
        ?.let {
            ChatMetaData(
                ItemId(it.itemID),
                Sat(it.sats_per_minute),
                it.ts,
                it.speed
            )
        }
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun ChatMetaData.toJson(moshi: Moshi): String =
    moshi.adapter(ChatMetaDataMoshi::class.java)
        .toJson(
            ChatMetaDataMoshi(
                itemId.value,
                satsPerMinute.value,
                timeSeconds,
                speed
            )
        )

data class ChatMetaData(
    val itemId: ItemId,
    val satsPerMinute: Sat,
    val timeSeconds: Int,
    val speed: Double,
)

// "{\"itemID\":1922435539,\"sats_per_minute\":3,\"ts\":4, \"speed\":1.5}"
@JsonClass(generateAdapter = true)
internal data class ChatMetaDataMoshi(
    val itemID: Long,
    val sats_per_minute: Long,
    val ts: Int,
    val speed: Double,
)
