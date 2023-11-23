package chat.sphinx.example.wrapper_mqtt

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class HopsDto(
   val pubkey: List<String>
)

@Throws(AssertionError::class)
fun HopsDto.toJson(moshi: Moshi): String {
   val adapter = moshi.adapter(HopsDto::class.java)
   return adapter.toJson(this)
}

fun String.hopsDtoOrNull(moshi: Moshi): KeyExchangeMessageDto? {
   return try {
      this.toHopsDto(moshi)
   } catch (e: Exception) {
      null
   }
}

@Throws(JsonDataException::class)
fun String.toHopsDto(moshi: Moshi): KeyExchangeMessageDto {
   val adapter = moshi.adapter(KeyExchangeMessageDto::class.java)
   return adapter.fromJson(this) ?: throw IllegalArgumentException("Invalid JSON for HopsDto")
}
