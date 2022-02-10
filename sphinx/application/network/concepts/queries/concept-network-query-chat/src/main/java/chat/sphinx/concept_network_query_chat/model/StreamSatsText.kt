package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@Throws(AssertionError::class)
fun StreamSatsText.toJson(moshi: Moshi): String =
    moshi.adapter(StreamSatsText::class.java)
        .toJson(
            StreamSatsText(
                feedID,
                itemID,
                ts,
                speed,
                uuid
            )
        )

@JsonClass(generateAdapter = true)
data class StreamSatsText(
    val feedID: String,
    val itemID: String,
    val ts: Long,
    val speed: Double,
    val uuid: String? = null,
)