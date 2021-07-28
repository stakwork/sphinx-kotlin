package chat.sphinx.wrapper_message

import chat.sphinx.wrapper_message_media.MessageMedia
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import okio.EOFException

@Suppress("NOTHING_TO_INLINE")
inline fun String.toGiphyDataOrNull(moshi: Moshi): GiphyData? =
    try {
        this.toGiphyData(moshi)
    } catch (e: Exception) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class, JsonDataException::class)
inline fun String.toGiphyData(moshi: Moshi): GiphyData =
    moshi.adapter(GiphyData::class.java)
        .fromJson(this)
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Suppress("NOTHING_TO_INLINE")
@Throws(AssertionError::class, EOFException::class)
inline fun GiphyData.toJson(moshi: Moshi): String =
    moshi.adapter(GiphyData::class.java)
        .toJson(this)

@Suppress("NOTHING_TO_INLINE")
inline fun GiphyData.retrieveImageUrlAndMessageMedia(): Pair<String, MessageMedia?>? {
    return if (url.isNotEmpty()) {
        Pair(url.replace("giphy.gif", "200w.gif"), null)
    } else {
        null
    }
}
// {"text": null,"url":"https://media3.giphy.com/media/StuemRSudGjTuu5QmY/giphy.gif?cid=d7a0fde9f4pgso0ojwyp6utwh63iiu9biqicdby6kv210sz5&rid=giphy.gif", "id":"StuemRSudGjTuu5QmY","aspect_ratio":"1.3259668508287292"}
@JsonClass(generateAdapter = true)
data class GiphyData(
    val id: String,
    val url: String,
    val aspect_ratio: Double,
    val text: String?,
) {
    companion object {
        const val MESSAGE_PREFIX = "giphy::"
    }
}
