package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toVirtualLightningNodeAddress(): VirtualLightningNodeAddress? =
    try {
        VirtualLightningNodeAddress(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun VirtualLightningNodeAddress.getPubKey(): LightningNodePubKey? {
    val elements = this.value.split(":")
    if (elements.size > 1) {
        return elements[0].toLightningNodePubKey()
    }
    return this.value.toLightningNodePubKey()
}

@Suppress("NOTHING_TO_INLINE")
inline fun VirtualLightningNodeAddress.getRouteHint(): LightningRouteHint? {
    val elements = this.value.split(":")
    if (elements.size == 3) {
        return "${elements[1]}:${elements[2]}".toLightningRouteHint()
    }
    return null
}

inline val String.isValidVirtualNodeAddress: Boolean
    get() = isNotEmpty() && matches("^${VirtualLightningNodeAddress.REGEX}\$".toRegex())


@JvmInline
value class VirtualLightningNodeAddress(val value: String) {

    companion object {
        const val REGEX = "${LightningNodePubKey.REGEX}:${LightningRouteHint.REGEX}"
    }

    init {
        require(value.isValidVirtualNodeAddress) {
            "Invalid VirtualLightningNodeAddress string value"
        }
    }
}