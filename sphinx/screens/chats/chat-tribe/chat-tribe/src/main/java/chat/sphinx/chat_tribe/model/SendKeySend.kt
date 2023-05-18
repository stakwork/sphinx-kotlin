package chat.sphinx.chat_tribe.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class SendKeySend(
    val password: String,
    val type: String,
    val application: String,
    val success: Boolean
)


@Throws(AssertionError::class)
fun SendKeySend.toJson(moshi: Moshi): String =
    moshi.adapter(SendKeySend::class.java)
        .toJson(
            SendKeySend(
                password,
                type,
                application,
                success
            )
        )