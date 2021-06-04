package chat.sphinx.wrapper_lightning

import chat.sphinx.wrapper_common.lightning.Sat

data class NodeBalanceAll(
    val localBalance: Sat,
    val remoteBalance: Sat,
)
