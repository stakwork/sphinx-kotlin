package chat.sphinx.example.concept_connect_manager

import chat.sphinx.example.concept_connect_manager.model.OwnerInfo
import chat.sphinx.wrapper_contact.NewContact
import chat.sphinx.wrapper_lightning.WalletMnemonic
import kotlinx.coroutines.flow.StateFlow

abstract class ConnectManager {

    abstract val ownerInfoStateFlow: StateFlow<OwnerInfo?>

    abstract fun createAccount()
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
        amount: Long?
    )

    abstract fun deleteMessage(
        sphinxMessage: String,
        contactPubKey: String,
    )

    abstract fun connectToTribe(
        tribeHost: String,
        tribePubKey: String,
        tribeRouteHint: String
    )

    abstract fun generateMediaToken(
        contactPubKey: String,
        muid: String,
        host: String,
        metaData: String?
    ): String?

    abstract fun setLspIp(ip: String)
    abstract fun retrieveLspIp(): String?
    abstract fun addListener(listener: ConnectManagerListener): Boolean
    abstract fun removeListener(listener: ConnectManagerListener): Boolean
    abstract fun processChallengeSignature(challenge: String)
}

interface ConnectManagerListener {

    fun onMnemonicWords(words: String)
    fun onOwnerRegistered(okKey: String, routeHint: String)
    fun onNewContactRegistered(msgSender: String)
    fun onMessageReceived(
        msg: String,
        msgSender: String,
        msgType: Int,
        msgUuid: String,
        msgIndex: String,
        amount: Long?
    )

    fun onMessageSent(
        msg: String,
        contactPubKey: String,
        msgType: Int,
        msgUUID: String,
        msgIndex: String,
    )

    fun onMessageUUID(msgUUID: String, provisionalId: Long)

    fun onUpdateUserState(userState: String)

    fun onSignedChallenge(sign: String)

    fun onNewBalance(balance: Long)

}


