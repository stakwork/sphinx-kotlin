package chat.sphinx.feature_coredb.adapters.invite

import chat.sphinx.wrapper_invite.InviteCode
import com.squareup.sqldelight.ColumnAdapter

internal class InviteCodeAdapter: ColumnAdapter<InviteCode, String> {
    override fun decode(databaseValue: String): InviteCode {
        return InviteCode(databaseValue)
    }

    override fun encode(value: InviteCode): String {
        return value.value
    }
}
