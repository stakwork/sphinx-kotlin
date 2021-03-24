package chat.sphinx.wrapper_common.chat

inline class MetaDataId(val value: Long) {
    init {
        require(value >= 0) {
            "MetaDataId must be greater than or equal to 0"
        }
    }
}
