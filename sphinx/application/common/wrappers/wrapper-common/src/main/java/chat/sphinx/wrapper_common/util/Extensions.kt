package chat.sphinx.wrapper_common.util

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

//DateTime
@Suppress("NOTHING_TO_INLINE")
inline fun Long.getHHMMSSString(): String {
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

@Suppress("NOTHING_TO_INLINE")
inline fun Long.getHHMMString(): String {
    val minutes = this / 1000 / 60
    val seconds = this / 1000 % 60

    return "${"%02d".format(minutes)}:${"%02d".format(seconds)}"
}