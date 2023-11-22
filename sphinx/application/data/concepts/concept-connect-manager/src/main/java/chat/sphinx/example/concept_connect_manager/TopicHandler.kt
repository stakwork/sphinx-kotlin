package chat.sphinx.example.concept_connect_manager

import chat.sphinx.example.concept_connect_manager.model.ConnectionState

sealed class TopicHandler {
    abstract fun handle(key: String, index: Int, action: String, message: String): ConnectionState

    object RegisterHandler: TopicHandler() {
        override fun handle(
            key: String,
            index: Int,
            action: String,
            message: String
        ): ConnectionState {
            return if (index == 0) {
                ConnectionState.OwnerRegistered(message)
            } else {
                ConnectionState.ContactRegistered(index, message)
            }
        }
    }

    object BalanceHandler: TopicHandler(){
        override fun handle(
            key: String,
            index: Int,
            action: String,
            message: String
        ): ConnectionState {
            TODO("Not yet implemented")
        }
    }
}