package chat.sphinx.example.concept_connect_manager

import chat.sphinx.example.concept_connect_manager.model.OwnerInfo
import chat.sphinx.wrapper_contact.NewContact
import chat.sphinx.wrapper_lightning.WalletMnemonic
import kotlinx.coroutines.flow.StateFlow

abstract class ConnectManager {

    abstract val ownerInfoStateFlow: StateFlow<OwnerInfo?>

    abstract fun createAccount(lspIp: String)
    abstract fun setInviteCode(inviteString: String)
    abstract fun setMnemonicWords(words: List<String>?)

    abstract fun createContact(contact: NewContact)
    abstract fun initializeMqttAndSubscribe(
        serverUri: String,
        mnemonicWords: WalletMnemonic,
        ownerInfo: OwnerInfo
    )
    abstract fun sendMessage(
        sphinxMessage: String,
        contactPubKey: String,
        provisionalId: Long,
        messageType: Int,
        amount: Long?,
        isTribe: Boolean = false
    )

    abstract fun deleteMessage(
        sphinxMessage: String,
        contactPubKey: String,
        isTribe: Boolean
    )

    abstract fun joinToTribe(
        tribeHost: String,
        tribePubKey: String,
        tribeRouteHint: String,
        isPrivate: Boolean
    )

    abstract fun createTribe(
        tribeServerPubKey: String,
        tribeJson: String
    )

    abstract fun createInvite(
        nickname: String,
        welcomeMessage: String,
        sats: Long,
        tribeServerPubKey: String?
    ): Pair<String, String>? // inviteString, inviteCode

    abstract fun createInvoice(
        amount: Long,
        memo: String
    ): Pair<String, String>? // invoice, paymentHash

    abstract fun processInvoicePayment(paymentRequest: String)

    abstract fun retrievePaymentHash(paymentRequest: String): String?

    abstract fun retrieveTribeMembersList(
        tribeServerPubKey: String,
        tribePubKey: String
    )

    abstract fun generateMediaToken(
        contactPubKey: String,
        muid: String,
        host: String,
        metaData: String?,
        amount: Long?
    ): String?

    abstract fun retrieveLspIp(): String?
    abstract fun addListener(listener: ConnectManagerListener): Boolean
    abstract fun removeListener(listener: ConnectManagerListener): Boolean
    abstract fun processChallengeSignature(challenge: String)
}

interface ConnectManagerListener {

    fun onMnemonicWords(words: String)
    fun onOwnerRegistered(okKey: String, routeHint: String)
    fun onMessageReceived(
        msg: String,
        msgSender: String,
        msgType: Int,
        msgUuid: String,
        msgIndex: String,
        amount: Long?,
        msgTimestamp: Long?
    )

    fun onMessageSent(
        msg: String,
        contactPubKey: String,
        msgType: Int,
        msgUUID: String,
        msgIndex: String,
        msgTimestamp: Long?,
    )

    fun onNewTribe(newTribe: String)

    fun onTribeMembersList(tribeMembers: String)

    fun onMessageUUID(msgUUID: String, provisionalId: Long)

    fun onUpdateUserState(userState: String)

    fun onDeleteUserState(userState: List<String>)

    fun onSignedChallenge(sign: String)

    fun onNewBalance(balance: Long)

    fun onNetworkStatusChange(isConnected: Boolean)

    fun onNewInviteCreated(inviteString: String)

}


