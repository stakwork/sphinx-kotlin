package chat.sphinx.dto_common.invite

inline class InviteId(val value: Long) {
    init {
        require(value > 0) {
            "InviteId must be greater than 0"
        }
    }
}
