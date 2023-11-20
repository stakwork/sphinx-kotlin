package chat.sphinx.wrapper_common.contact

@Suppress("NOTHING_TO_INLINE")
inline fun Long.toContactIndex(): ContactIndex? =
    try {
        ContactIndex(this)
    } catch (e: IllegalArgumentException) {
        null
    }


@JvmInline
value class ContactIndex(val value: Long) {

    init {
        require(this.value >= 0L) {
            "ContactIndex must be greater than or equal 0"
        }
    }

}
