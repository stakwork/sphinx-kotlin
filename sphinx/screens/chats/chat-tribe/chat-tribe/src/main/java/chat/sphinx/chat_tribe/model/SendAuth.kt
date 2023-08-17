package chat.sphinx.chat_tribe.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class SendAuth(
    val budget: Int,
    val pubkey: String,
    val type: String,
    val application: String,
    val password: String,
    val signature: String?
)
@Throws(AssertionError::class)
fun SendAuth.toJson(moshi: Moshi): String =
    moshi.adapter(SendAuth::class.java)
        .toJson(
            SendAuth(
                budget,
                pubkey,
                type,
                application,
                password,
                signature
            )
        )