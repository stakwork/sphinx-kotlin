package chat.sphinx.wrapper_invite

@Suppress("NOTHING_TO_INLINE")
inline fun String.toValidInviteStringOrNull(): InviteString? =
    try {
        InviteString(this).let { iString ->
            if (iString.isValid) {
                iString
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }

// Don't want to make this a requirement for InviteString init
// block as it could cause crashes in the persistence layer due
// to it being a non-null required argument in the InviteDbo.
inline val InviteString.isValid: Boolean
    get() = value.matches("^[A-F0-9a-f]{40}\$".toRegex())

@JvmInline
value class InviteString(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "InviteString cannot be empty"
        }
    }
}
