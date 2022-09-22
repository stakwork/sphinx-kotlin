package chat.sphinx.chat_common.model

import chat.sphinx.chat_common.ui.viewstate.messageholder.LayoutState
import chat.sphinx.chat_common.util.SphinxLinkify
import chat.sphinx.chat_common.util.isSphinxUrl
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import chat.sphinx.wrapper_common.tribe.isValidTribeJoinLink
import chat.sphinx.wrapper_common.tribe.toTribeJoinLink

inline val String.isNodeUrl: Boolean
    get() = isValidLightningNodePubKey ||
            isValidVirtualNodeAddress

sealed interface MessageLinkPreview {
    companion object {
        internal fun parse(
            text: LayoutState.Bubble.ContainerThird.Message?,
            urlLinkPreviewsEnabled: Boolean
        ): MessageLinkPreview? {
            if (text == null) {
                return null
            }

            text.text?.let { url ->


                val matcher = SphinxLinkify.SphinxPatterns.AUTOLINK_WEB_URL.matcher(url)

                return if (url.isNodeUrl) {

                    url.toLightningNodePubKey()?.let { nnKey ->

                        NodeDescriptor(nnKey)

                    } ?: url.toVirtualLightningNodeAddress()?.let { nnAddress ->

                        NodeDescriptor(nnAddress)

                    }

                } else if (matcher.find()) {

                    val group = matcher.group()

                    group.toTribeJoinLink()?.let { nnTribeLink ->

                        TribeLink(nnTribeLink)

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
                } else null
            }
            return null
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
 * When a url is shared
 * */
@JvmInline
value class UnspecifiedUrl(val url: String): MessageLinkPreview
