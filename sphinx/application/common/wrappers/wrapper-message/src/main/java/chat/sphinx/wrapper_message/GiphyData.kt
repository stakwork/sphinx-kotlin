package chat.sphinx.wrapper_message

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@Suppress("NOTHING_TO_INLINE")
inline fun String.toGiphyDataOrNull(moshi: Moshi): GiphyData? =
    try {
        this.toGiphyData(moshi)
    } catch (e: Exception) {
        null
    }

fun String.toGiphyData(moshi: Moshi): GiphyData =
    moshi.adapter(GiphyData::class.java)
        .fromJson(this)
        ?: throw IllegalArgumentException("Provided Json was invalid")

@Throws(AssertionError::class)
fun GiphyData.toJson(moshi: Moshi): String =
    moshi.adapter(GiphyData::class.java)
        .toJson(this)

// {"text": null,"url":"https://media3.giphy.com/media/StuemRSudGjTuu5QmY/giphy.gif?cid=d7a0fde9f4pgso0ojwyp6utwh63iiu9biqicdby6kv210sz5&rid=giphy.gif", "id":"StuemRSudGjTuu5QmY","aspect_ratio":"1.3259668508287292"}
@JsonClass(generateAdapter = true)
data class GiphyData(
    val id: String,
    val url: String,
    val aspect_ratio: Double,
    val text: String?,
)
