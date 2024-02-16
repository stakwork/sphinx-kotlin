package chat.sphinx.transactions.ui.viewstate

import chat.sphinx.wrapper_message.Message


internal sealed class TransactionHolderViewState(
    val transaction: String? = null,
    val invoice: Message? = null,
    val messageSenderName: String? = null,
) {

    class Loader : TransactionHolderViewState()

    class Outgoing(
        transaction: String,
        invoice: Message?,
        messageSenderName: String?
    ) : TransactionHolderViewState(
        transaction,
        invoice,
        messageSenderName
    )

    class Incoming(
        transaction: String,
        invoice: Message?,
        messageSenderName: String?
    ) : TransactionHolderViewState(
        transaction,
        invoice,
        messageSenderName
    )

    sealed class Failed(
        transaction: String,
        invoice: Message?,
        messageSenderName: String?
    ) : TransactionHolderViewState(
        transaction,
        invoice,
        messageSenderName
    ) {
        class Open(
            transaction: String,
            invoice: Message?,
            messageSenderName: String?
        ): Failed(
            transaction,
            invoice,
            messageSenderName
        )

        class Closed(
            transaction: String,
            invoice: Message?,
            messageSenderName: String?
        ): Failed(
            transaction,
            invoice,
            messageSenderName
        )
    }

}
