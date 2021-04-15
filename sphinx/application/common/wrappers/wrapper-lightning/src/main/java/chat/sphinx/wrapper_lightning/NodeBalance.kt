package chat.sphinx.wrapper_lightning

import chat.sphinx.wrapper_common.lightning.Sat

data class NodeBalance(
    val reserve: Sat,
    val fullBalance: Sat,
    val balance: Sat,
    val pendingOpenBalance: Sat,
)
