package chat.sphinx.feature_coredb.adapters.action_track

import chat.sphinx.wrapper_action_track.*
import chat.sphinx.wrapper_chat.ChatPrivate
import chat.sphinx.wrapper_chat.toChatPrivate
import chat.sphinx.wrapper_common.feed.toFeedType
import com.squareup.sqldelight.ColumnAdapter

internal class ActionTrackIdAdapter: ColumnAdapter<ActionTrackId, Long> {

    override fun decode(databaseValue: Long): ActionTrackId {
        return ActionTrackId(databaseValue)
    }

    override fun encode(value: ActionTrackId): Long {
        return value.value
    }
}

internal class ActionTrackTypeAdapter: ColumnAdapter<ActionTrackType, Long> {

    override fun decode(databaseValue: Long): ActionTrackType {
        return databaseValue.toInt().toActionTrackType()
    }

    override fun encode(value: ActionTrackType): Long {
        return value.value.toLong()
    }
}

internal class ActionTrackMetaDataAdapter: ColumnAdapter<ActionTrackMetaData, String> {

    override fun decode(databaseValue: String): ActionTrackMetaData {
        return ActionTrackMetaData(databaseValue)
    }

    override fun encode(value: ActionTrackMetaData): String {
        return value.value
    }
}

internal class ActionTrackUploadedAdapter: ColumnAdapter<ActionTrackUploaded, Long> {
    override fun decode(databaseValue: Long): ActionTrackUploaded {
        return databaseValue.toInt().toActionTrackUploaded()
    }

    override fun encode(value: ActionTrackUploaded): Long {
        return value.value.toLong()
    }
}

