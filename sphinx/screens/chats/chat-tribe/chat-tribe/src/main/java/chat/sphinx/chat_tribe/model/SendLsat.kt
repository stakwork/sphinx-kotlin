package chat.sphinx.chat_tribe.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class SendLsat(
    val password: String,
    val budget: String,
    val type: String,
    val application: String,
    val lsat: String?,
    val success: Boolean
)


@Throws(AssertionError::class)
fun SendLsat.toJson(moshi: Moshi): String =
    moshi.adapter(SendLsat::class.java)
        .toJson(
            SendLsat(
                password,
                budget,
                type,
                application,
                lsat,
                success
            )
        )