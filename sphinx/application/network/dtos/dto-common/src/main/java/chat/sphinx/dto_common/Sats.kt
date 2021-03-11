package chat.sphinx.dto_common

inline class Sats(val value: Long) {
    init {
        require(value >= 0L) {
            "Sats must be greater than or equal to 0"
        }
    }
}