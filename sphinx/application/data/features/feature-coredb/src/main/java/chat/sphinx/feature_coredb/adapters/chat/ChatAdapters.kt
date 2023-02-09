package chat.sphinx.feature_coredb.adapters.chat

import chat.sphinx.feature_coredb.adapters.common.DateTimeAdapter
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.lightning.Sat
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.ColumnAdapter

internal class ChatUUIDAdapter: ColumnAdapter<ChatUUID, String> {
    override fun decode(databaseValue: String): ChatUUID {
        return ChatUUID(databaseValue)
    }

    override fun encode(value: ChatUUID): String {
        return value.value
    }
}

internal class ChatNameAdapter: ColumnAdapter<ChatName, String> {
    override fun decode(databaseValue: String): ChatName {
        return ChatName(databaseValue)
    }

    override fun encode(value: ChatName): String {
        return value.value
    }
}

internal class ChatTypeAdapter: ColumnAdapter<ChatType, Long> {
    override fun decode(databaseValue: Long): ChatType {
        return databaseValue.toInt().toChatType()
    }

    override fun encode(value: ChatType): Long {
        return value.value.toLong()
    }
}

internal class ChatStatusAdapter: ColumnAdapter<ChatStatus, Long> {
    override fun decode(databaseValue: Long): ChatStatus {
        return databaseValue.toInt().toChatStatus()
    }

    override fun encode(value: ChatStatus): Long {
        return value.value.toLong()
    }
}

internal class ChatMutedAdapter private constructor(): ColumnAdapter<ChatMuted, Long> {

    companion object {
        @Volatile
        private var instance: ChatMutedAdapter? = null
        fun getInstance(): ChatMutedAdapter =
            instance ?: synchronized(this) {
                instance ?: ChatMutedAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): ChatMuted {
        return databaseValue.toInt().toChatMuted()
    }

    override fun encode(value: ChatMuted): Long {
        return value.value.toLong()
    }
}

internal class ChatGroupKeyAdapter: ColumnAdapter<ChatGroupKey, String> {
    override fun decode(databaseValue: String): ChatGroupKey {
        return ChatGroupKey(databaseValue)
    }

    override fun encode(value: ChatGroupKey): String {
        return value.value
    }
}

internal class ChatHostAdapter: ColumnAdapter<ChatHost, String> {
    override fun decode(databaseValue: String): ChatHost {
        return ChatHost(databaseValue)
    }

    override fun encode(value: ChatHost): String {
        return value.value
    }
}

internal class ChatUnlistedAdapter: ColumnAdapter<ChatUnlisted, Long> {
    override fun decode(databaseValue: Long): ChatUnlisted {
        return databaseValue.toInt().toChatUnlisted()
    }

    override fun encode(value: ChatUnlisted): Long {
        return value.value.toLong()
    }
}

internal class ChatPrivateAdapter: ColumnAdapter<ChatPrivate, Long> {
    override fun decode(databaseValue: Long): ChatPrivate {
        return databaseValue.toInt().toChatPrivate()
    }

    override fun encode(value: ChatPrivate): Long {
        return value.value.toLong()
    }
}

internal class ChatMetaDataAdapter(val moshi: Moshi): ColumnAdapter<ChatMetaData, String> {
    override fun decode(databaseValue: String): ChatMetaData {
        return databaseValue.toChatMetaData(moshi)
    }

    override fun encode(value: ChatMetaData): String {
        return value.toJson(moshi)
    }
}

internal class ChatAliasAdapter: ColumnAdapter<ChatAlias, String> {
    override fun decode(databaseValue: String): ChatAlias {
        return ChatAlias(databaseValue)
    }

    override fun encode(value: ChatAlias): String {
        return value.value
    }
}

internal class NotifyAdapter: ColumnAdapter<NotificationLevel, Long> {
    override fun decode(databaseValue: Long): NotificationLevel {
        return databaseValue.toInt().toNotificationLevel()
    }

    override fun encode(value: NotificationLevel): Long {
        return value.value.toLong()
    }
}
