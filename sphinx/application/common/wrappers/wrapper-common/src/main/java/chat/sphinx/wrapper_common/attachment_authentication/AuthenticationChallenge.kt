package chat.sphinx.wrapper_common.attachment_authentication

import chat.sphinx.wrapper_common.lightning.LightningNodePubKey

@Suppress("NOTHING_TO_INLINE")
inline fun String.toAuthenticationChallenge(): AuthenticationChallenge? =
    try {
        AuthenticationChallenge(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class AuthenticationChallenge(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AuthenticationChallenge cannot be empty"
        }
    }
}