package chat.sphinx.chat_tribe.model

data class SendAuth(
    val budget: String,
    val pubkey: String,
    val type: String,
    val application: String,
    val password: String
)

@Suppress("NOTHING_TO_INLINE")
inline fun SendAuth.generateSendAuthString(): String =
    "window.sphinxMessage(\\'{\\\"password\\\":\\\"${this.password}\\\",\\\"type\\\":\\\"${this.type}\\\",\\\"budget\\\":${this.budget},\\\"application\\\":\\\"${this.application}\\\",\\\"pubkey\\\":\\\"${this.pubkey}\\\"}\\')"

fun generateRandomPass(): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..16)
        .map { allowedChars.random() }
        .joinToString("")
}