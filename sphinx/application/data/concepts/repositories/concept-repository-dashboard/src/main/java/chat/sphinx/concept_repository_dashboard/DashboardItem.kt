package chat.sphinx.concept_repository_dashboard

import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.dashboard.InviteId
import chat.sphinx.wrapper_common.message.MessageId

// TODO: Rework model to handle
//  DashboardDbo.contact_id
//  DashboardDbo.id
//  DashboardDbo.date
//  DashboardDbo.display_name
//  DashboardDbo.include_in_return
//  DashboardDbo.latest_message_id
//  DashboardDbo.muted
//  DashboardDbo.photo_url
//  DashboardDbo.seen
sealed class DashboardItem {

    sealed class Active: DashboardItem() {

        abstract val chatId: ChatId
        abstract val messageId: MessageId?

        data class Conversation(
            override val chatId: ChatId,
            val contactId: ContactId,
            override val messageId: MessageId?
        ): Active()

        data class GroupOrTribe(
            override val chatId: ChatId,
            override val messageId: MessageId?
        ): Active()

    }

    /**
     * Inactive chats are for newly added contacts that are awaiting
     * messages to be sent (the Chat has not been created yet)
     * */
    sealed class Inactive: DashboardItem() {

        data class Conversation(
            val contactId: ContactId,
        ): Inactive()

        data class PendingInvite(
            val inviteId: InviteId,
        ): Inactive()
    }
}