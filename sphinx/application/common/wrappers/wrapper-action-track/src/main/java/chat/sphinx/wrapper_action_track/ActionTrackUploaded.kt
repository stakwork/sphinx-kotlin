package chat.sphinx.wrapper_action_track

@Suppress("NOTHING_TO_INLINE")
inline fun ActionTrackUploaded.isTrue(): Boolean =
    this is ActionTrackUploaded.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int?.toActionTrackUploaded(): ActionTrackUploaded =
    when (this) {
        null,
        ActionTrackUploaded.UPLOADED -> {
            ActionTrackUploaded.True
        }
        else -> {
            ActionTrackUploaded.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean?.toActionTrackUploaded(): ActionTrackUploaded =
    if (this == false) ActionTrackUploaded.False else ActionTrackUploaded.True


sealed class ActionTrackUploaded {

    companion object {
        const val UPLOADED = 1
        const val NOT_UPLOADED = 0
    }

    abstract val value: Int

    object True: ActionTrackUploaded() {
        override val value: Int
            get() = UPLOADED
    }

    object False: ActionTrackUploaded() {
        override val value: Int
            get() = NOT_UPLOADED
    }
}