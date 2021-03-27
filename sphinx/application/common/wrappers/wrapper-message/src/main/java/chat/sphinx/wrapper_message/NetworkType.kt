@file:Suppress("SpellCheckingInspection")

package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun NetworkType.isLightning(): Boolean =
    this is NetworkType.Lightning

@Suppress("NOTHING_TO_INLINE")
inline fun NetworkType.isMqtt(): Boolean =
    this is NetworkType.Mqtt

@Suppress("NOTHING_TO_INLINE")
inline fun NetworkType.isUnknown(): Boolean =
    this is NetworkType.Unknown

/**
 * Converts the integer value returned over the wire to an object.
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int.toNetworkType(): NetworkType =
    when (this) {
        NetworkType.LIGHTNING -> {
            NetworkType.Lightning
        }
        NetworkType.MQTT -> {
            NetworkType.Mqtt
        }
        else -> {
            NetworkType.Unknown(this)
        }
    }

/**
 * Comes off the wire as:
 *  - 0 (Lightning)
 *  - 1 (Mqtt)
 *
 * https://github.com/stakwork/sphinx-relay/blob/7f8fd308101b5c279f6aac070533519160aa4a9f/src/constants.ts#L63
 * */
sealed class NetworkType {

    companion object {
        const val LIGHTNING = 0
        const val MQTT = 1
    }

    abstract val value: Int

    object Lightning: NetworkType() {
        override val value: Int
            get() = LIGHTNING
    }

    object Mqtt: NetworkType() {
        override val value: Int
            get() = MQTT
    }

    class Unknown(override val value: Int) : NetworkType()
}
