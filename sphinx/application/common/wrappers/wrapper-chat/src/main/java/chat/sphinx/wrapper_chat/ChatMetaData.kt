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

@Throws(
    NumberFormatException::class,
    IndexOutOfBoundsException::class,
    IllegalArgumentException::class
)
fun String.toChatMetaData(): ChatMetaData {
//    Off the wire:
//    "{\"itemID\":1922435539,\"sats_per_minute\":3,\"ts\":4, \"speed\":1.5}"
    val splits = this
        .replace('"', ' ')
        .replace('\\', ' ')
        .replace('{', ' ')
        .replace('}', ' ')
        .replace("\\s".toRegex(), "")
        .split(',')

    val id: MetaDataId = splits[0].split(':').let { idSplit ->
        if (idSplit[0] != "itemID") {
            throw IllegalArgumentException("MetaData string did not contain 'itemID' field")
        }

        MetaDataId(idSplit[1].toLong())
    }

    val sats: Sat = splits[1].split(':').let { satSplit ->
        if (satSplit[0] != "sats_per_minute") {
            throw IllegalArgumentException("MetaData string did not contain 'sats_per_minute' field")
        }

        Sat(satSplit[1].toLong())
    }

    val timeSeconds: Int = splits[2].split(':').let { timeSplit ->
        if (timeSplit[0] != "ts") {
            throw IllegalArgumentException("MetaData string did not contain 'ts' field")
        }

        timeSplit[1].toInt()
    }

    val speed: Double = splits[3].split(':').let { speedSplit ->
        if (speedSplit[0] != "speed") {
            throw IllegalArgumentException("MetaData string did not contain 'speed' field")
        }

        speedSplit[1].toDouble()
    }

    return ChatMetaData(id, sats, timeSeconds, speed)
}

data class ChatMetaData(
    val itemId: MetaDataId,
    val satsPerMinute: Sat,
    val timeSeconds: Int,
    val speed: Double,
) {
    override fun toString(): String {
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
}
