package chat.sphinx.example.wrapper_mqtt

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi


@JsonClass(generateAdapter = true)
data class LastReadMessages(
    val values: Map<String, Long>?
) {
    companion object {
        fun String.toLastReadMessages(moshi: Moshi): LastReadMessages? {
            val adapter: JsonAdapter<LastReadMessages> = moshi.adapter(LastReadMessages::class.java)
            return try {
                adapter.fromJson(this)
            } catch (e: Exception) {
                null
            }
        }
    }
}
