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
value class LightningNodePubKey(override val value: String): LightningNodeDescriptor {

    companion object {

        const val REGEX = "[A-F0-9a-f]{66}"

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
