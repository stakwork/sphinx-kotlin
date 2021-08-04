package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toVirtualLightningNodePubKey(): VirtualLightningNodePubKey? =
    try {
        VirtualLightningNodePubKey(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun VirtualLightningNodePubKey.getPubKey(): LightningNodePubKey? {
    val elements = this.value.split(":")
    if (elements.size > 1) {
        return elements[0].toLightningNodePubKey()
    }
    return this.value.toLightningNodePubKey()
}

@Suppress("NOTHING_TO_INLINE")
inline fun VirtualLightningNodePubKey.getRouteHint(): LightningRouteHint? {
    val elements = this.value.split(":")
    if (elements.size == 3) {
        return "${elements[1]}:${elements[2]}".toLightningRouteHint()
    }
    return null
}

inline val String.isValidVirtualLightningNodePubKey: Boolean
    get() = isNotEmpty() && matches("^${VirtualLightningNodePubKey.VIRTUAL_NODE_PUB_KEY_REGEX}\$".toRegex())


@JvmInline
value class VirtualLightningNodePubKey(val value: String) {

    companion object {

        const val VIRTUAL_NODE_PUB_KEY_REGEX = "[A-F0-9a-f]{66}:[A-F0-9a-f]{66}:[0-9]+"

        fun fromByteArray(byteArray: ByteArray): LightningNodePubKey {
            return LightningNodePubKey(byteArray.decodeToString())
        }
    }

    init {
        require(value.isValidVirtualLightningNodePubKey) {
            "Invalid Lightning Node Public Key"
        }
    }
}