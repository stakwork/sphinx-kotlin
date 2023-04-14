package chat.sphinx.chat_common.model

import chat.sphinx.chat_common.ui.viewstate.messageholder.LayoutState
import chat.sphinx.chat_common.util.SphinxLinkify
import chat.sphinx.wrapper_common.feed.FeedItemLink
import chat.sphinx.wrapper_common.feed.toFeedItemLink
import chat.sphinx.wrapper_common.lightning.LightningNodeDescriptor
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toVirtualLightningNodeAddress
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink

sealed interface MessageLinkPreview {
    companion object {
        internal fun parse(
            text: LayoutState.Bubble.ContainerThird.Message?,
            urlLinkPreviewsEnabled: Boolean
        ): MessageLinkPreview? {
            if (text == null) {
                return null
            }

            val matcher = SphinxLinkify.SphinxPatterns.LINK_PREVIEWS.matcher(
                text.text ?: ""
            )
            return if (matcher.find()) {

                val group = matcher.group()

                group.toLightningNodePubKey()?.let { nnKey ->

                    NodeDescriptor(nnKey)

                } ?: group.toVirtualLightningNodeAddress()?.let { nnAddress ->

                    NodeDescriptor(nnAddress)

                } ?: group.toTribeJoinLink()?.let { nnTribeLink ->

                    TribeLink(nnTribeLink)

                } ?: group.toFeedItemLink()?.let { nnFeedItemLink ->

                    FeedItemPreview(nnFeedItemLink)

                } ?: run {
                    if (!urlLinkPreviewsEnabled) {
                        null
                    } else {
                        when {
                            group.startsWith("http://") || group.startsWith("https://") -> {
                                UnspecifiedUrl(group)
                            }
                            else -> {
                                UnspecifiedUrl("https://$group")
                            }
                        }
                    }
                }

            } else {

                null

            }
        }
    }
}

/**
 * When a contact is shared
 * */
@JvmInline
value class NodeDescriptor(val nodeDescriptor: LightningNodeDescriptor): MessageLinkPreview

/**
 * When a tribe link is shared
 * */
@JvmInline
value class TribeLink(val tribeJoinLink: TribeJoinLink): MessageLinkPreview

/**
 * When a feed item is shared
 * */
@JvmInline
value class FeedItemPreview(val feedItemLink: FeedItemLink): MessageLinkPreview

/**
 * When a url is shared
 * */
@JvmInline
value class UnspecifiedUrl(val url: String): MessageLinkPreview


