package chat.sphinx.wrapper_contact

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.contact.ContactIndex
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.ShortChannelId

data class NewContact(
    val contactAlias: ContactAlias?,
    val lightningNodePubKey: LightningNodePubKey?,
    val lightningRouteHint: LightningRouteHint?,
    val childPubKey: LightningNodePubKey?,
    val index: ContactIndex,
    val scid: ShortChannelId?,
    val ownLspPubKey: LightningNodePubKey?,
    val contactKey: LightningNodePubKey?,
    val contactRouteHint: LightningRouteHint?,
    val photoUrl: PhotoUrl?
    )