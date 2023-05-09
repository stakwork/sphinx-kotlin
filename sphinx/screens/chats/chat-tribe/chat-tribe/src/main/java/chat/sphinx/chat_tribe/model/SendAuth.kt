package chat.sphinx.chat_tribe.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class SendAuth(
    val budget: Int,
    val pubkey: String,
    val type: String,
    val application: String,
    val password: String
)

@Suppress("NOTHING_TO_INLINE")
inline fun SendAuth.generateSendAuthString(): String =
    "window.sphinxMessage(\\\'{\\\"password\\\":\\\"${this.password}\\\",\\\"type\\\":\\\"${this.type}\\\",\\\"budget\\\":${this.budget},\\\"application\\\":\\\"${this.application}\\\",\\\"pubkey\\\":\\\"${this.pubkey}\\\"}\\\')"

@Throws(AssertionError::class)
fun SendAuth.toJson(moshi: Moshi): String =
    moshi.adapter(SendAuth::class.java)
        .toJson(
            SendAuth(
                budget,
                pubkey,
                type,
                application,
                password
            )
        )