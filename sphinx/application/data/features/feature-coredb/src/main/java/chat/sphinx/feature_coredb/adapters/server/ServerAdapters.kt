package chat.sphinx.feature_coredb.adapters.server

import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.ServerIp
import com.squareup.sqldelight.ColumnAdapter


internal class IpAdapter: ColumnAdapter<ServerIp, String> {

    override fun decode(databaseValue: String): ServerIp {
        return ServerIp(databaseValue)
    }

    override fun encode(value: ServerIp): String {
        return value.value
    }
}