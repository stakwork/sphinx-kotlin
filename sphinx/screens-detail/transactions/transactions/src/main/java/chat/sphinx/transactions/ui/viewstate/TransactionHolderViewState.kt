package chat.sphinx.transactions.ui.viewstate

import chat.sphinx.concept_network_query_message.model.TransactionDto
import chat.sphinx.wrapper_message.Message


internal sealed class TransactionHolderViewState(
    val transaction: TransactionDto? = null,
    val invoice: Message? = null,
    val messageSenderName: String? = null,
) {

    class Loader : TransactionHolderViewState()

    class Outgoing(
        transaction: TransactionDto,
        invoice: Message?,
        messageSenderName: String?
    ) : TransactionHolderViewState(
        transaction,
        invoice,
        messageSenderName
    )

    class Incoming(
        transaction: TransactionDto,
        invoice: Message?,
        messageSenderName: String?
    ) : TransactionHolderViewState(
        transaction,
        invoice,
        messageSenderName
    )

    sealed class Failed(
        transaction: TransactionDto,
        invoice: Message?,
        messageSenderName: String?
    ) : TransactionHolderViewState(
        transaction,
        invoice,
        messageSenderName
    ) {
        class Open(
            transaction: TransactionDto,
            invoice: Message?,
            messageSenderName: String?
        ): Failed(
            transaction,
            invoice,
            messageSenderName
        )

        class Closed(
            transaction: TransactionDto,
            invoice: Message?,
            messageSenderName: String?
        ): Failed(
            transaction,
            invoice,
            messageSenderName
        )
    }

}
