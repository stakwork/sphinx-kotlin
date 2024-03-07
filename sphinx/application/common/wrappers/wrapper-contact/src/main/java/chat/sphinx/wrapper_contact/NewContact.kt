package chat.sphinx.wrapper_contact

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.Sat

data class NewContact(
    val contactAlias: ContactAlias?,
    val lightningNodePubKey: LightningNodePubKey?,
    val lightningRouteHint: LightningRouteHint?,
    val photoUrl: PhotoUrl?,
    val confirmed: Boolean,
    val inviteString: String?,
    val inviteCode: String?,
    val invitePrice: Sat?
    )