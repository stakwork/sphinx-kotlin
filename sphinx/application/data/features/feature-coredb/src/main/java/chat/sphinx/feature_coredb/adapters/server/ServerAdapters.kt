package chat.sphinx.feature_coredb.adapters.server

import chat.sphinx.wrapper_common.server.LspPubKey
import chat.sphinx.wrapper_common.server.ServerIp
import com.squareup.sqldelight.ColumnAdapter


internal class IpAdapter: ColumnAdapter<ServerIp, String> {

    override fun decode(databaseValue: String): ServerIp {
        return ServerIp(databaseValue)
    }

    override fun encode(value: ServerIp): String {
        return value.value
    }
}

internal class PubKeyAdapter: ColumnAdapter<LspPubKey, String> {

    override fun decode(databaseValue: String): LspPubKey {
        return LspPubKey(databaseValue)
    }

    override fun encode(value: LspPubKey): String {
        return value.value
    }
}
