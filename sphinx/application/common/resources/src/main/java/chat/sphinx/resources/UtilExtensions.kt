package chat.sphinx.resources

@Suppress("NOTHING_TO_INLINE")
inline fun Long.toTimestamp(): String {
    val minutes = this / 1000 / 60
    val seconds = this / 1000 % 60

    return "${"%02d".format(minutes)}:${"%02d".format(seconds)}"
}