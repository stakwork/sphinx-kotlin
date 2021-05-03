package chat.sphinx.concept_socket_io

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_invite.model.InviteDto
import chat.sphinx.concept_network_query_lightning.model.invoice.InvoiceDto
import chat.sphinx.concept_network_query_message.model.MessageDto

sealed class SphinxSocketIOMessage {

    sealed class Type<T>: SphinxSocketIOMessage() {

        abstract val dto: T

        class ChatSeen(override val dto: ChatDto): Type<ChatDto>() {
            companion object {
                const val JSON_TYPE = "chat_seen"
            }
        }

        class Contact(override val dto: ContactDto): Type<ContactDto>() {
            companion object {
                const val JSON_TYPE = "contact"
            }
        }

        class Invite(override val dto: InviteDto): Type<InviteDto>() {
            companion object {
                const val JSON_TYPE = "invite"
            }
        }

        class InvoicePayment(override val dto: InvoiceDto): Type<InvoiceDto>() {
            companion object {
                const val JSON_TYPE = "invoice_payment"
            }
        }

        sealed class MessageType: Type<MessageDto>() {

            class Boost(override val dto: MessageDto): MessageType() {
                companion object {
                    const val JSON_TYPE = "boost"
                }
            }

            class Delete(override val dto: MessageDto): MessageType() {
                companion object {
                    const val JSON_TYPE = "delete"
                }
            }

            sealed class Group: MessageType() {

                class Create(override val dto: MessageDto): Group() {
                    companion object {
                        const val JSON_TYPE = "group_create"
                    }
                }

                class Leave(override val dto: MessageDto): Group() {
                    companion object {
                        const val JSON_TYPE = "group_leave"
                    }
                }

                class Join(override val dto: MessageDto): Group() {
                    companion object {
                        const val JSON_TYPE = "group_join"
                    }
                }

                class Kick(override val dto: MessageDto): Group() {
                    companion object {
                        const val JSON_TYPE = "group_kick"
                    }
                }

                class TribeDelete(override val dto: MessageDto): Group() {
                    companion object {
                        const val JSON_TYPE = "tribe_delete"
                    }
                }

                sealed class Member: Group() {

                    class Request(override val dto: MessageDto): Member() {
                        companion object {
                            const val JSON_TYPE = "member_request"
                        }
                    }

                    class Approve(override val dto: MessageDto): Member() {
                        companion object {
                            const val JSON_TYPE = "member_approve"
                        }
                    }

                    class Reject(override val dto: MessageDto): Member() {
                        companion object {
                            const val JSON_TYPE = "member_reject"
                        }
                    }

                }
            }

            class KeySend(override val dto: MessageDto): MessageType() {
                companion object {
                    const val JSON_TYPE = "keysend"
                }
            }

            class Message(override val dto: MessageDto): MessageType() {
                companion object {
                    const val JSON_TYPE = "message"
                }
            }

            class Purchase(override val dto: MessageDto): MessageType() {
                companion object {
                    const val JSON_TYPE = "purchase"
                }
            }

            class PurchaseAccept(override val dto: MessageDto): MessageType() {
                companion object {
                    const val JSON_TYPE = "purchase_accept"
                }
            }

            class PurchaseDeny(override val dto: MessageDto): MessageType() {
                companion object {
                    const val JSON_TYPE = "purchase_deny"
                }
            }

        }

    }

}

interface SphinxSocketIOMessageListener {

    /**
     * All exceptions thrown when [msg] is dispatched to this method
     * are caught and logged.
     *
     * Method is called from [kotlinx.coroutines.Dispatchers.IO]
     * */
    suspend fun onSocketIOMessageReceived(msg: SphinxSocketIOMessage)
}
