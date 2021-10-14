package chat.sphinx.chat_common.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class ChatNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver)
{
    abstract suspend fun toPaymentSendDetail(contactId: ContactId, chatId: ChatId?)

    abstract suspend fun toPaymentReceiveDetail(contactId: ContactId, chatId: ChatId?)

    abstract suspend fun toAddContactDetail(
        pubKey: LightningNodePubKey? = null,
        routeHint: LightningRouteHint? = null
    )

    abstract suspend fun toJoinTribeDetail(tribeLink: TribeJoinLink)

    @JvmSynthetic
    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(
            PopBackStack()
        )
    }

    protected abstract suspend fun toChatContact(chatId: ChatId?, contactId: ContactId)
    protected abstract suspend fun toChatGroup(chatId: ChatId)
    protected abstract suspend fun toChatTribe(chatId: ChatId)

    @JvmSynthetic
    internal suspend fun toChat(chat: Chat?, contactId: ContactId?) {
        if (chat == null) {
            contactId?.let { nnContactId ->
                toChatContact(null, nnContactId)
            }
        } else {
            when (chat.type) {
                is ChatType.Conversation -> {
                    contactId?.let { nnContactId ->
                        toChatContact(chat.id, nnContactId)
                    }
                }
                is ChatType.Group -> {
                    toChatGroup(chat.id)
                }
                is ChatType.Tribe -> {
                    toChatTribe(chat.id)
                }
                else -> {}
            }
        }
    }

    @JvmSynthetic
    internal suspend fun toFullscreenVideo(
        messageId: MessageId,
        videoFilepath: String? = null
    ) {
        navigationDriver.submitNavigationRequest(
            ToFullscreenVideoActivity(
                messageId,
                videoFilepath
            )
        )
    }
}
