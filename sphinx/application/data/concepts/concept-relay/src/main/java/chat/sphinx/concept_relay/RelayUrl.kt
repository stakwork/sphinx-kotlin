package chat.sphinx.concept_relay

inline class RelayUrl(val value: String){
    init {
        require(value.isNotEmpty()) {
            "RelayUrl cannot be empty"
        }
    }
}