package chat.sphinx.feature_coredb.adapters.invite

import chat.sphinx.wrapper_invite.InviteString
import com.squareup.sqldelight.ColumnAdapter

internal class InviteStringAdapter: ColumnAdapter<InviteString, String> {
    override fun decode(databaseValue: String): InviteString {
        return InviteString(databaseValue)
    }

    override fun encode(value: InviteString): String {
        return value.value
    }
}
