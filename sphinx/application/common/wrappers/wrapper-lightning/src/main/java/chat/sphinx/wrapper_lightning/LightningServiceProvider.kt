package chat.sphinx.wrapper_lightning

import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.ServerIp

data class LightningServiceProvider(
    val ip: ServerIp,
    val pubKey: LightningNodePubKey?
)
