package chat.sphinx.wrapper_common.util

import okio.base64.decodeBase64ToArray

@Suppress("NOTHING_TO_INLINE")
inline fun String.getInitials(charLimit: Int = 2): String {
    val sb = StringBuilder()
    this.split(' ').let { splits ->
        for ((index, split) in splits.withIndex()) {
            if (index < charLimit) {
                sb.append(split.firstOrNull() ?: "")
            } else {
                break
            }
        }
    }
    return sb.toString()
}

//Media
@Suppress("NOTHING_TO_INLINE")
inline fun String.getHostFromMediaToken(): String? {
    this.getMediaTokenElementWithIndex(0)?.let { mediaToken ->
        return mediaToken
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.getMUIDFromMediaToken(): String? {
    this.getMediaTokenElementWithIndex(1)?.let { muid ->
        return muid
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.getMediaAttributeWithName(name: String): String? {
    this.getMediaTokenElementWithIndex(4)?.let { metaData ->
        metaData.split("&").let { metaDataItems ->
            for (item in metaDataItems) {
                if (item.contains("$name=")) {
                    return item.replace("$name=", "")
                }
            }
        }
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.getMediaTokenElementWithIndex(index: Int): String? {
    this.split(".").filter { it.isNotBlank() }.let { splits ->
        if (splits.size > index) {
            val element = splits[index]
            return element.decodeBase64ToArray()?.toString(charset("UTF-8"))
        }
    }
    return null
}

//DateTime
@Suppress("NOTHING_TO_INLINE")
inline fun Long.getTimeString(): String {
    val hours: Int
    val minutes: Int
    var seconds: Int = this.toInt() / 1000

    hours = seconds / 3600
    minutes = seconds / 60 % 60
    seconds %= 60

    val hoursString = if (hours < 10) "0${hours}" else "$hours"
    val minutesString = if (minutes < 10) "0${minutes}" else "$minutes"
    val secondsString = if (seconds < 10) "0${seconds}" else "$seconds"

    return "$hoursString:$minutesString:$secondsString"
}