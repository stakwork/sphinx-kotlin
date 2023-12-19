package chat.sphinx.example.concept_connect_manager.model

sealed class TopicHandler {
    abstract fun handle(topic: String, index: Int, message: String, payload: ByteArray?): ConnectionState

    companion object {
        fun retrieveConnectionState(
            topic: String,
            message: String,
            payload: ByteArray?
        ): ConnectionState? {
            val parts = topic.split("/")
            val index = parts[1].toIntOrNull() ?: return null
            val action = parts.getOrNull(3) ?: return null

            return when (action) {
                "register" -> RegisterHandler.handle(topic, index, message, payload)
                "balance" -> BalanceHandler.handle(topic, index, message, payload)
                "stream" -> StreamHandler.handle(topic, index, message, payload)
                "send" -> SendHandler.handle(topic, index, message, payload)
                "msgs"-> MessageHandler.handle(topic, index, message, payload)
                "settle"-> SettleHandler.handle(topic, index, message, payload)
                else -> throw IllegalArgumentException("Unknown topic key: $topic")
            }
        }
    }
    object SettleHandler: TopicHandler() {
        override fun handle(
            topic: String,
            index: Int,
            message: String,
            payload: ByteArray?
        ): ConnectionState {
            return ConnectionState.OnionMessage(index, payload)
        }
    }

    object MessageHandler: TopicHandler() {
        override fun handle(
            topic: String,
            index: Int,
            message: String,
            payload: ByteArray?
        ): ConnectionState {
            return ConnectionState.OnionMessage(index, payload)
        }

    }

    object SendHandler: TopicHandler() {
        override fun handle(
            topic: String,
            index: Int,
            message: String,
            payload: ByteArray?
        ): ConnectionState {
            return ConnectionState.OnionMessage(index, payload)
        }
    }

    object RegisterHandler: TopicHandler() {
        override fun handle(
            topic: String,
            index: Int,
            message: String,
            payload: ByteArray?
        ): ConnectionState {
            return if (index == 0) {
                ConnectionState.OwnerRegistered(message)
            } else {
                val childPubKey = topic.split("/").getOrNull(0)
                ConnectionState.ContactRegistered(index, message, childPubKey)
            }
        }
    }

    object StreamHandler: TopicHandler() {
        override fun handle(
            topic: String,
            index: Int,
            message: String,
            payload: ByteArray?
        ): ConnectionState {
            val parts = topic.split("/")

            val htlcId = parts.getOrNull(4)
            val rHash = parts.getOrNull(5)
            val mSat = parts.getOrNull(6)

             return if (rHash != null) {
                ConnectionState.KeySend(index, message, rHash)
            } else {
                ConnectionState.OnionMessage(index, payload)
            }
        }
    }

    object BalanceHandler: TopicHandler(){
        override fun handle(
            topic: String,
            index: Int,
            message: String,
            payload: ByteArray?
        ): ConnectionState {
            TODO("Not yet implemented")
        }
    }
}