package chat.sphinx.wrapper_chat

import chat.sphinx.wrapper_common.chat.MetaDataId
import chat.sphinx.wrapper_common.lightning.Sat

@Suppress("NOTHING_TO_INLINE")
inline fun String.toChatMetaDataOrNull(): ChatMetaData? =
    try {
        this.toChatMetaData()
    } catch (e: Exception) {
        null
    }

/**
 *
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(NumberFormatException::class, IndexOutOfBoundsException::class)
inline fun String.toChatMetaData(): ChatMetaData {
//    Off the wire:
//    "{\"itemID\":1922435539,\"sats_per_minute\":3,\"ts\":4, \"speed\":1.5}"
    val splits = this
        .replace('"', ' ')
        .replace('\\', ' ')
        .replace('{', ' ')
        .replace('}', ' ')

        // ChatMetaData.toString
        .replace('=', ':')

        .replace("\\s".toRegex(), "")
        .split(',')

    return ChatMetaData(
        MetaDataId(splits[0].split(':')[1].toLong()),
        Sat(splits[1].split(':')[1].toLong()),
        splits[2].split(':')[1].toInt(),
        splits[3].split(':')[1].toDouble()
    )
}

@Suppress("NOTHING_TO_INLINE")
inline fun ChatMetaData.toRelayString(): String {
//    "{\"itemID\":1922435539,\"sats_per_minute\":3,\"ts\":4, \"speed\":1.5}"
    val sb = StringBuilder()
    sb.append("\"{")
    sb.append("\\\"itemID\\\":")
    sb.append("${itemId.value},")
    sb.append("\\\"sats_per_minute\\\":")
    sb.append("${satsPerMinute.value},")
    sb.append("\\\"ts\\\":")
    sb.append("$timeSeconds, ")
    sb.append("\\\"speed\\\":")
    sb.append("$speed")
    sb.append("}\"")
    return sb.toString()
}

data class ChatMetaData(
    val itemId: MetaDataId,
    val satsPerMinute: Sat,
    val timeSeconds: Int,
    val speed: Double,
)
