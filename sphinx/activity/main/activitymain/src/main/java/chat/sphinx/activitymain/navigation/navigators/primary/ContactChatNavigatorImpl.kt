package chat.sphinx.activitymain.navigation.navigators.primary

import chat.sphinx.activitymain.R
import chat.sphinx.activitymain.navigation.drivers.DetailNavigationDriver
import chat.sphinx.activitymain.navigation.drivers.PrimaryNavigationDriver
import chat.sphinx.chat_contact.navigation.ContactChatNavigator
import chat.sphinx.chat_contact.navigation.ToChatContactScreen
import chat.sphinx.chat_group.navigation.ToChatGroupScreen
import chat.sphinx.chat_tribe.navigation.ToChatTribeScreen
import chat.sphinx.contact_detail.navigation.ToContactDetailScreen
import chat.sphinx.join_tribe.navigation.ToJoinTribeDetail
import chat.sphinx.new_contact.navigation.ToNewContactDetail
import chat.sphinx.payment_send.navigation.ToPaymentSendDetail
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatType
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import javax.inject.Inject

internal class ContactChatNavigatorImpl @Inject constructor(
    navigationDriver: PrimaryNavigationDriver,
    private val detailDriver: DetailNavigationDriver,
): ContactChatNavigator(navigationDriver) {
    override suspend fun toPaymentSendDetail(contactId: ContactId, chatId: ChatId?) {
        detailDriver.submitNavigationRequest(ToPaymentSendDetail(contactId, chatId))
    }

    override suspend fun toChatDetail(chatId: ChatId, contactId: ContactId?) {
        detailDriver.submitNavigationRequest(ToContactDetailScreen(chatId, contactId))
    }

    override suspend fun toAddContactDetail(pubKey: LightningNodePubKey) {
        detailDriver.submitNavigationRequest(
            ToNewContactDetail(pubKey, false)
        )
    }

    override suspend fun toJoinTribeDetail(tribeLink: TribeJoinLink) {
        detailDriver.submitNavigationRequest(ToJoinTribeDetail(tribeLink))
    }

    override suspend fun toChatContact(chatId: ChatId?, contactId: ContactId) {
        navigationDriver.submitNavigationRequest(
            ToChatContactScreen(
                chatId = chatId,
                contactId = contactId,
                popUpToId = R.id.navigation_dashboard_fragment,
                popUpToInclusive = false,
            )
        )
    }

    override suspend fun toChatGroup(chatId: ChatId) {
        navigationDriver.submitNavigationRequest(
            ToChatGroupScreen(
                chatId = chatId,
                popUpToId = R.id.navigation_dashboard_fragment,
                popUpToInclusive = false,
            )
        )
    }

    override suspend fun toChatTribe(chatId: ChatId) {
        navigationDriver.submitNavigationRequest(
            ToChatTribeScreen(
                chatId = chatId,
                popUpToId = R.id.navigation_dashboard_fragment,
                popUpToInclusive = false,
            )
        )
    }
}
