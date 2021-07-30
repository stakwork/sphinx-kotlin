package chat.sphinx.chat_common.navigation

import androidx.navigation.NavController
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class ChatNavigator(
    navigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(navigationDriver)
{
    abstract suspend fun toPaymentSendDetail(contactId: ContactId, chatId: ChatId?)

    abstract suspend fun toChatDetail(chatId: ChatId, contactId: ContactId? = null)

    abstract suspend fun toAddContactDetail(pubKey: LightningNodePubKey)

    abstract suspend fun toChat(chat: Chat?, contactId: ContactId?)

    abstract suspend fun toJoinTribeDetail(tribeLink: TribeJoinLink)

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(
            PopBackStack()
        )
    }
}
