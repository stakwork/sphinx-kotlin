package chat.sphinx.wrapper_action_track

@Suppress("NOTHING_TO_INLINE")
inline fun String.toActionTrackMetaData(): ActionTrackMetaData? =
    try {
        ActionTrackMetaData(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ActionTrackMetaData(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ActionTrackMetaData cannot be empty"
        }
    }
}