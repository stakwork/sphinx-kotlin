package chat.sphinx.concept_network_client

interface NetworkClientClearedListener {
    /**
     * This is called *before* the client gets cleared and the executor
     * service is shutdown.
     * */
    fun networkClientCleared()
}
