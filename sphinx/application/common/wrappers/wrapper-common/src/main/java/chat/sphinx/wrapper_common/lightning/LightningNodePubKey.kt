package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningNodePubKey(): LightningNodePubKey? =
    try {
        LightningNodePubKey(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidLightningNodePubKey: Boolean
    get() = isNotEmpty() && matches("^${LightningNodePubKey.REGEX}\$".toRegex())

@JvmInline
value class LightningNodePubKey(val value: String) {

    companion object {

        private const val PUB_KEY_REGEX = "[A-F0-9a-f]{66}"
        private const val VIRTUAL_NODE_PUB_KEY_REGEX = "[A-F0-9a-f]{66}:[A-F0-9a-f]{66}:[0-9]+"

        const val REGEX = "($VIRTUAL_NODE_PUB_KEY_REGEX|$PUB_KEY_REGEX)"

        fun fromByteArray(byteArray: ByteArray): LightningNodePubKey {
            return LightningNodePubKey(byteArray.decodeToString())
        }
    }

    init {
        require(value.isValidLightningNodePubKey) {
            "Invalid Lightning Node Public Key"
        }
    }
}
