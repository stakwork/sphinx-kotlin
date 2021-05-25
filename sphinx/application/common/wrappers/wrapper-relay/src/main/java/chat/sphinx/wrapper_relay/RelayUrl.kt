package chat.sphinx.wrapper_relay

@JvmInline
value class RelayUrl(val value: String){
    init {
        require(value.isNotEmpty()) {
            "RelayUrl cannot be empty"
        }
    }
}
