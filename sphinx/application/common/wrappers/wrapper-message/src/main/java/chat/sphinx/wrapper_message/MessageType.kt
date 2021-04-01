package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isMessage(): Boolean =
    this is MessageType.Message

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isConfirmation(): Boolean =
    this is MessageType.Confirmation

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isInvoice(): Boolean =
    this is MessageType.Invoice

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isPayment(): Boolean =
    this is MessageType.Payment

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isCancellation(): Boolean =
    this is MessageType.Cancellation

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isDirectPayment(): Boolean =
    this is MessageType.DirectPayment

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isAttachment(): Boolean =
    this is MessageType.Attachment

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isPurchase(): Boolean =
    this is MessageType.Purchase

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isPurchaseAccept(): Boolean =
    this is MessageType.PurchaseAccept

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isPurchaseDeny(): Boolean =
    this is MessageType.PurchaseDeny

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isContactKey(): Boolean =
    this is MessageType.ContactKey

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isContactKeyConfirmation(): Boolean =
    this is MessageType.ContactKeyConfirmation

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isGroupCreate(): Boolean =
    this is MessageType.GroupCreate

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isGroupInvite(): Boolean =
    this is MessageType.GroupInvite

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isGroupJoin(): Boolean =
    this is MessageType.GroupJoin

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isGroupLeave(): Boolean =
    this is MessageType.GroupLeave

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isGroupKick(): Boolean =
    this is MessageType.GroupKick

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isDelete(): Boolean =
    this is MessageType.Delete

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isRepayment(): Boolean =
    this is MessageType.Repayment

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isMemberRequest(): Boolean =
    this is MessageType.MemberRequest

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isMemberApprove(): Boolean =
    this is MessageType.MemberApprove

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isMemberReject(): Boolean =
    this is MessageType.MemberReject

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isTribeDelete(): Boolean =
    this is MessageType.TribeDelete

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isBotInstall(): Boolean =
    this is MessageType.BotInstall

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isBotCmd(): Boolean =
    this is MessageType.BotCmd

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isBotRes(): Boolean =
    this is MessageType.BotRes

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isHeartbeat(): Boolean =
    this is MessageType.Heartbeat

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isHeartbeatConfirmation(): Boolean =
    this is MessageType.HeartbeatConfirmation

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isKeySend(): Boolean =
    this is MessageType.KeySend

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isBoost(): Boolean =
    this is MessageType.Boost

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isQuery(): Boolean =
    this is MessageType.Query

@Suppress("NOTHING_TO_INLINE")
inline fun MessageType.isQueryResponse(): Boolean =
    this is MessageType.QueryResponse

/**
 * Converts the integer value returned over the wire to an object.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun Int.toMessageType(): MessageType =
    when (this) {
        MessageType.MESSAGE -> {
            MessageType.Message
        }
        MessageType.CONFIRMATION -> {
            MessageType.Confirmation
        }
        MessageType.INVOICE -> {
            MessageType.Invoice
        }
        MessageType.PAYMENT -> {
            MessageType.Payment
        }
        MessageType.CANCELLATION -> {
            MessageType.Cancellation
        }
        MessageType.DIRECT_PAYMENT -> {
            MessageType.DirectPayment
        }
        MessageType.ATTACHMENT -> {
            MessageType.Attachment
        }
        MessageType.PURCHASE -> {
            MessageType.Purchase
        }
        MessageType.PURCHASE_ACCEPT -> {
            MessageType.PurchaseAccept
        }
        MessageType.PURCHASE_DENY -> {
            MessageType.PurchaseDeny
        }
        MessageType.CONTACT_KEY -> {
            MessageType.ContactKey
        }
        MessageType.CONTACT_KEY_CONFIRMATION -> {
            MessageType.ContactKeyConfirmation
        }
        MessageType.GROUP_CREATE -> {
            MessageType.GroupCreate
        }
        MessageType.GROUP_INVITE -> {
            MessageType.GroupInvite
        }
        MessageType.GROUP_JOIN -> {
            MessageType.GroupJoin
        }
        MessageType.GROUP_LEAVE -> {
            MessageType.GroupLeave
        }
        MessageType.GROUP_KICK -> {
            MessageType.GroupKick
        }
        MessageType.DELETE -> {
            MessageType.Delete
        }
        MessageType.REPAYMENT -> {
            MessageType.Repayment
        }
        MessageType.MEMBER_REQUEST -> {
            MessageType.MemberRequest
        }
        MessageType.MEMBER_APPROVE -> {
            MessageType.MemberApprove
        }
        MessageType.MEMBER_REJECT -> {
            MessageType.MemberReject
        }
        MessageType.TRIBE_DELETE -> {
            MessageType.TribeDelete
        }
        MessageType.BOT_INSTALL -> {
            MessageType.BotInstall
        }
        MessageType.BOT_CMD -> {
            MessageType.BotCmd
        }
        MessageType.BOT_RES -> {
            MessageType.BotRes
        }
        MessageType.HEARTBEAT -> {
            MessageType.Heartbeat
        }
        MessageType.HEARTBEAT_CONFIRMATION -> {
            MessageType.HeartbeatConfirmation
        }
        MessageType.KEY_SEND -> {
            MessageType.KeySend
        }
        MessageType.BOOST -> {
            MessageType.Boost
        }
        MessageType.QUERY -> {
            MessageType.Query
        }
        MessageType.QUERY_RESPONSE -> {
            MessageType.QueryResponse
        }
        else -> {
            MessageType.Unknown(this)
        }
    }

/**
 * Comes off the wire as:
 *  - 0 (Message)
 *  - 1 (Confirmation)
 *  - 2 (Invoice)
 *  - 3 (Payment)
 *  - 4 (Cancellation)
 *  - 5 (Direct Payment)
 *  - 6 (Attachment)
 *  - 7 (Purchase)
 *  - 8 (Purchase Accept)
 *  - 9 (Purchase Deny)
 *  - 10 (Contact Key)
 *  - 11 (Contact Key Confirmation)
 *  - 12 (Group Create)
 *  - 13 (Group Invite)
 *  - 14 (Group Join)
 *  - 15 (Group Leave)
 *  - 16 (Group Kick)
 *  - 17 (Delete)
 *  - 18 (Repayment)
 *  - 19 (Member Request)
 *  - 20 (Member Approve)
 *  - 21 (Member Reject)
 *  - 22 (Tribe Delete)
 *  - 23 (Bot Install)
 *  - 24 (Bot Cmd)
 *  - 25 (Bot Res)
 *  - 26 (Heartbeat)
 *  - 27 (Heartbeat Confirmation)
 *  - 28 (Key Send)
 *  - 29 (Boost)
 *  - 30 (Query)
 *  - 31 (Query Response)
 *
 * https://github.com/stakwork/sphinx-relay/blob/7f8fd308101b5c279f6aac070533519160aa4a9f/src/constants.ts#L29
 * */
sealed class MessageType {

    companion object {
        const val MESSAGE = 0 // SHOW
        const val CONFIRMATION = 1
        const val INVOICE = 2 // SHOW
        const val PAYMENT = 3 // SHOW
        const val CANCELLATION = 4
        const val DIRECT_PAYMENT = 5 // SHOW
        const val ATTACHMENT = 6 // SHOW
        const val PURCHASE = 7
        const val PURCHASE_ACCEPT = 8
        const val PURCHASE_DENY = 9
        const val CONTACT_KEY = 10
        const val CONTACT_KEY_CONFIRMATION = 11
        const val GROUP_CREATE = 12
        const val GROUP_INVITE = 13
        const val GROUP_JOIN = 14 // SHOW
        const val GROUP_LEAVE = 15 // SHOW
        const val GROUP_KICK = 16
        const val DELETE = 17
        const val REPAYMENT = 18
        const val MEMBER_REQUEST = 19
        const val MEMBER_APPROVE = 20
        const val MEMBER_REJECT = 21
        const val TRIBE_DELETE = 22
        const val BOT_INSTALL = 23
        const val BOT_CMD = 24
        const val BOT_RES = 25 // SHOW
        const val HEARTBEAT = 26
        const val HEARTBEAT_CONFIRMATION = 27
        const val KEY_SEND = 28
        const val BOOST = 29 // SHOW
        const val QUERY = 30
        const val QUERY_RESPONSE = 31

        const val SHOW = true
        const val DO_NOT_SHOW = false
    }

    abstract val show: Boolean
    abstract val value: Int

    object Message: MessageType() {
        override val show: Boolean
            get() = SHOW

        override val value: Int
            get() = MESSAGE
    }

    object Confirmation: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = CONFIRMATION
    }

    object Invoice: MessageType() {
        override val show: Boolean
            get() = SHOW

        override val value: Int
            get() = INVOICE
    }

    object Payment: MessageType() {
        override val show: Boolean
            get() = SHOW

        override val value: Int
            get() = PAYMENT
    }

    object Cancellation: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = CANCELLATION
    }

    object DirectPayment: MessageType() {
        override val show: Boolean
            get() = SHOW

        override val value: Int
            get() = DIRECT_PAYMENT
    }

    object Attachment: MessageType() {
        override val show: Boolean
            get() = SHOW

        override val value: Int
            get() = ATTACHMENT
    }

    object Purchase: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = PURCHASE
    }

    object PurchaseAccept: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = PURCHASE_ACCEPT
    }

    object PurchaseDeny: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = PURCHASE_DENY
    }

    object ContactKey: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = CONTACT_KEY
    }

    object ContactKeyConfirmation: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = CONTACT_KEY_CONFIRMATION
    }

    object GroupCreate: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = GROUP_CREATE
    }

    object GroupInvite: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = GROUP_INVITE
    }

    object GroupJoin: MessageType() {
        override val show: Boolean
            get() = SHOW

        override val value: Int
            get() = GROUP_JOIN
    }

    object GroupLeave: MessageType() {
        override val show: Boolean
            get() = SHOW

        override val value: Int
            get() = GROUP_LEAVE
    }

    object GroupKick: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = GROUP_KICK
    }

    object Delete: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = DELETE
    }

    object Repayment: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = REPAYMENT
    }

    object MemberRequest: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = MEMBER_REQUEST
    }

    object MemberApprove: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = MEMBER_APPROVE
    }

    object MemberReject: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = MEMBER_REJECT
    }

    object TribeDelete: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = TRIBE_DELETE
    }

    object BotInstall: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = BOT_INSTALL
    }

    object BotCmd: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = BOT_CMD
    }

    object BotRes: MessageType() {
        override val show: Boolean
            get() = SHOW

        override val value: Int
            get() = BOT_RES
    }

    object Heartbeat: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = HEARTBEAT
    }

    object HeartbeatConfirmation: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = HEARTBEAT_CONFIRMATION
    }

    object KeySend: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = KEY_SEND
    }

    object Boost: MessageType() {
        override val show: Boolean
            get() = SHOW

        override val value: Int
            get() = BOOST
    }

    object Query: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = QUERY
    }

    object QueryResponse: MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW

        override val value: Int
            get() = QUERY_RESPONSE
    }

    class Unknown(override val value: Int) : MessageType() {
        override val show: Boolean
            get() = DO_NOT_SHOW
    }
}
