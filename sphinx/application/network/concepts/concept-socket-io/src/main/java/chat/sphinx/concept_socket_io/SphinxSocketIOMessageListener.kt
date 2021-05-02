package chat.sphinx.concept_socket_io

sealed class SphinxSocketIOMessageType {
    abstract val json: String
    class Boost(override val json: String) : SphinxSocketIOMessageType()
    class Contact(override val json: String): SphinxSocketIOMessageType()
    class Delete(override val json: String): SphinxSocketIOMessageType()
    class Message(override val json: String): SphinxSocketIOMessageType()
}

/**
 * This call happens on the [io.socket.engine.emitter.Emitter] background thread
 * such that it is main safe
 * */
interface SphinxSocketIOMessageListener {

    /**
     * All exceptions thrown when [type] is dispatched to this method
     * are caught and logged.
     *
     * Method is called from [kotlinx.coroutines.Dispatchers.IO]
     * */
    suspend fun onSocketIOMessageReceived(type: SphinxSocketIOMessageType)
}
