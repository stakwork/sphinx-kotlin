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

inline val String.isValidVirtualNodePubKey: Boolean
    get() = isNotEmpty() && matches("^${VirtualLightningNodePubKey.REGEX}\$".toRegex())


@JvmInline
value class VirtualLightningNodePubKey(val value: String) {

    companion object {
        const val REGEX = "${LightningNodePubKey.REGEX}:${LightningRouteHint.REGEX}"
    }

    init {
        require(value.isValidVirtualNodePubKey) {
            "Invalid Lightning Node Public Key"
        }
    }
}