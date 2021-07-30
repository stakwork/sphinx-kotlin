package chat.sphinx.onboard_common.model

import chat.sphinx.wrapper_common.lightning.LightningNodePubKey

data class OnBoardInviterData(
    val nickname: String?,
    val pubkey: LightningNodePubKey?,
    val routeHint: String?,
    val message: String?,
    val action: String?,
    val pin: String?
)
