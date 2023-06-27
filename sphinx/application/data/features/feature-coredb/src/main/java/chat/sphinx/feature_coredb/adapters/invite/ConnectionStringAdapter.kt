package chat.sphinx.feature_coredb.adapters.invite

import chat.sphinx.wrapper_invite.ConnectionString
import chat.sphinx.wrapper_invite.InviteString
import com.squareup.sqldelight.ColumnAdapter

internal class ConnectionStringAdapter: ColumnAdapter<ConnectionString, String> {
    override fun decode(databaseValue: String): ConnectionString {
        return ConnectionString(databaseValue)
    }

    override fun encode(value: ConnectionString): String {
        return value.value
    }
}
