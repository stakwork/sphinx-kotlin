package chat.sphinx.feature_repository.adapters.chat

import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.chat.ChatId
import com.squareup.sqldelight.ColumnAdapter

class ChatIdAdapter: ColumnAdapter<ChatId, Long> {
    override fun decode(databaseValue: Long): ChatId {
        return ChatId(databaseValue)
    }

    override fun encode(value: ChatId): Long {
        return value.value
    }
}

class ChatUUIDAdapter: ColumnAdapter<ChatUUID, String> {
    override fun decode(databaseValue: String): ChatUUID {
        return ChatUUID(databaseValue)
    }

    override fun encode(value: ChatUUID): String {
        return value.value
    }
}

class ChatNameAdapter: ColumnAdapter<ChatName, String> {
    override fun decode(databaseValue: String): ChatName {
        return ChatName(databaseValue)
    }

    override fun encode(value: ChatName): String {
        return value.value
    }
}

class ChatTypeAdapter: ColumnAdapter<ChatType, Long> {
    override fun decode(databaseValue: Long): ChatType {
        return databaseValue.toInt().toChatType()
    }

    override fun encode(value: ChatType): Long {
        return value.value.toLong()
    }
}

class ChatStatusAdapter: ColumnAdapter<ChatStatus, Long> {
    override fun decode(databaseValue: Long): ChatStatus {
        return databaseValue.toInt().toChatStatus()
    }

    override fun encode(value: ChatStatus): Long {
        return value.value.toLong()
    }
}

class ChatMutedAdapter: ColumnAdapter<ChatMuted, Long> {
    override fun decode(databaseValue: Long): ChatMuted {
        return databaseValue.toInt().toChatMuted()
    }

    override fun encode(value: ChatMuted): Long {
        return value.value.toLong()
    }
}

class ChatGroupKeyAdapter: ColumnAdapter<ChatGroupKey, String> {
    override fun decode(databaseValue: String): ChatGroupKey {
        return ChatGroupKey(databaseValue)
    }

    override fun encode(value: ChatGroupKey): String {
        return value.value
    }
}

class ChatHostAdapter: ColumnAdapter<ChatHost, String> {
    override fun decode(databaseValue: String): ChatHost {
        return ChatHost(databaseValue)
    }

    override fun encode(value: ChatHost): String {
        return value.value
    }
}

class ChatUnlistedAdapter: ColumnAdapter<ChatUnlisted, Long> {
    override fun decode(databaseValue: Long): ChatUnlisted {
        return databaseValue.toInt().toChatUnlisted()
    }

    override fun encode(value: ChatUnlisted): Long {
        return value.value.toLong()
    }
}

class ChatPrivateAdapter: ColumnAdapter<ChatPrivate, Long> {
    override fun decode(databaseValue: Long): ChatPrivate {
        return databaseValue.toInt().toChatPrivate()
    }

    override fun encode(value: ChatPrivate): Long {
        return value.value.toLong()
    }
}

class ChatMetaDataAdapter: ColumnAdapter<ChatMetaData, String> {
    override fun decode(databaseValue: String): ChatMetaData {
        return databaseValue.toChatMetaData()
    }

    override fun encode(value: ChatMetaData): String {
        return value.toRelayString()
    }
}

class ChatAliasAdapter: ColumnAdapter<ChatAlias, String> {
    override fun decode(databaseValue: String): ChatAlias {
        return ChatAlias(databaseValue)
    }

    override fun encode(value: ChatAlias): String {
        return value.value
    }
}
