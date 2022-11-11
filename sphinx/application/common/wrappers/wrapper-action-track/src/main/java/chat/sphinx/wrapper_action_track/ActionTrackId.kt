package chat.sphinx.wrapper_action_track

@JvmInline
value class ActionTrackId(val value: Long) {
    init {
        require(value >= 0) {
            "ActionTrackId must be greater than or equal to 0"
        }
    }
}
