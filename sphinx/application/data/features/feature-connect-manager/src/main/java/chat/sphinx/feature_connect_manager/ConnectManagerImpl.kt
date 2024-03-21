package chat.sphinx.feature_connect_manager

import android.util.Base64
import android.util.Log
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.example.concept_connect_manager.ConnectManagerListener
import chat.sphinx.example.concept_connect_manager.model.OwnerInfo
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningRouteHint
import chat.sphinx.wrapper_contact.NewContact
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import chat.sphinx.wrapper_message.MessageType
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPackDynamicSerializer
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.base64.encodeBase64
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONException
import org.json.JSONObject
import uniffi.sphinxrs.RunReturn
import uniffi.sphinxrs.addContact
import uniffi.sphinxrs.codeFromInvite
import uniffi.sphinxrs.getSubscriptionTopic
import uniffi.sphinxrs.getTribeManagementTopic
import uniffi.sphinxrs.handle
import uniffi.sphinxrs.initialSetup
import uniffi.sphinxrs.joinTribe
import uniffi.sphinxrs.listTribeMembers
import uniffi.sphinxrs.makeInvite
import uniffi.sphinxrs.makeMediaToken
import uniffi.sphinxrs.makeMediaTokenWithMeta
import uniffi.sphinxrs.makeMediaTokenWithPrice
import uniffi.sphinxrs.mnemonicFromEntropy
import uniffi.sphinxrs.mnemonicToSeed
import uniffi.sphinxrs.paymentHashFromInvoice
import uniffi.sphinxrs.processInvite
import uniffi.sphinxrs.rootSignMs
import uniffi.sphinxrs.send
import uniffi.sphinxrs.setBlockheight
import uniffi.sphinxrs.setNetwork
import uniffi.sphinxrs.signBytes
import uniffi.sphinxrs.xpubFromSeed
import java.security.SecureRandom
import java.util.Calendar
import kotlin.math.min
import kotlin.math.pow

class ConnectManagerImpl(
    dispatchers: CoroutineDispatchers
): ConnectManager(),
    CoroutineDispatchers by dispatchers
{
    private var mixerIp: String? = null
    private var walletMnemonic: WalletMnemonic? = null
    private var mqttClient: MqttAsyncClient? = null
    private val network = "regtest"
    private var ownerSeed: String? = null
    private var inviteCode: String? = null
    private var mnemonicWords: List<String>? = null
    private var inviterContact: NewContact? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _ownerInfoStateFlow: MutableStateFlow<OwnerInfo?> by lazy {
        MutableStateFlow(null)
    }
    override val ownerInfoStateFlow: StateFlow<OwnerInfo?>
        get() = _ownerInfoStateFlow.asStateFlow()

    // Key Generation and Management
    override fun createAccount(lspIp: String) {
        coroutineScope.launch {
            mixerIp = lspIp

            val seed = generateMnemonic()
            val now = getTimestampInMilliseconds()

            seed.first?.let { firstSeed ->
                val xPub = generateXPub(firstSeed, now, network)
                val sig = rootSignMs(firstSeed, now, network)
                ownerSeed = firstSeed

                if (xPub != null) {
                    var invite: RunReturn? = null

                    if (inviteCode != null) {
                        invite = processInvite(ownerSeed!!, now, getCurrentUserState(), inviteCode!!)
                        mixerIp = invite.lspHost
                    }

                    connectToMQTT(mixerIp!!, xPub, now, sig, invite)
                }
            }
        }
    }

    override fun setInviteCode(inviteString: String) {
        this.inviteCode = inviteString
    }

    override fun setMnemonicWords(words: List<String>?) {
        this.mnemonicWords = words
    }


    @OptIn(ExperimentalUnsignedTypes::class)
    private fun generateMnemonic(): Pair<String?, WalletMnemonic?> {
        var seed: String? = null

        // Check if is account restoration
        val mnemonic = if (!mnemonicWords.isNullOrEmpty()) {
            mnemonicWords!!.joinToString(" ").toWalletMnemonic()
        } else {
            try {
                val randomBytes = generateRandomBytes(16)
                val randomBytesString =
                    randomBytes.joinToString("") { it.toString(16).padStart(2, '0') }
                val words = mnemonicFromEntropy(randomBytesString)

                words.toWalletMnemonic()
            } catch (e: Exception) {
                null
            }
        }

        mnemonic?.value?.let { words ->
            try {
                seed = mnemonicToSeed(words)

                notifyListeners {
                    onMnemonicWords(words)
                }

            } catch (e: Exception) {}
        }

        return Pair(seed, mnemonic)
    }

    private fun generateXPub(seed: String, time: String, network: String): String? {
        return try {
            xpubFromSeed(seed, time, network)
        } catch (e: Exception) {
            null
        }
    }

    override fun createContact(
        contact: NewContact
    ) {
        coroutineScope.launch {
            val now = getTimestampInMilliseconds()

            try {
                val runReturn = addContact(
                    ownerSeed!!,
                    now,
                    getCurrentUserState(),
                    contact.lightningNodePubKey?.value!!,
                    contact.lightningRouteHint?.value!!,
                    ownerInfoStateFlow.value?.alias ?: "",
                    ownerInfoStateFlow.value?.picture ?: "",
                    3000.toULong(),
                    contact.inviteCode
                )

                handleRunReturn(
                    runReturn,
                    mqttClient!!
                )
            } catch (e: Exception) {
                Log.e("MQTT_MESSAGES", "add contact excp $e")
            }
        }
    }

    // MQTT Connection Management

    override fun initializeMqttAndSubscribe(
        serverUri: String,
        mnemonicWords: WalletMnemonic,
        ownerInfo: OwnerInfo
    ) {
        coroutineScope.launch {

            val seed = try {
                mnemonicToSeed(mnemonicWords.value)
            } catch (e: Exception) {
                null
            }

            val xPub = seed?.let {
                generateXPub(
                    it,
                    getTimestampInMilliseconds(),
                    network
                )
            }

            val now = getTimestampInMilliseconds()

            val sig = seed?.let {
                rootSignMs(
                    it,
                    now,
                    network
                )
            }

            if (xPub != null && sig != null) {

                mixerIp = serverUri
                walletMnemonic = mnemonicWords
                ownerSeed = seed
                _ownerInfoStateFlow.value = ownerInfo

                connectToMQTT(
                    serverUri,
                    xPub,
                    now,
                    sig,
                )
            }
        }
    }

    private fun connectToMQTT(
        serverURI: String,
        clientId: String,
        key: String,
        password: String,
        invite: RunReturn? = null
    ) {
        mqttClient = try {
            MqttAsyncClient(serverURI, clientId, null)
        } catch (e: MqttException) {
            e.printStackTrace()
            return
        }

        val options = MqttConnectOptions().apply {
            this.userName = key
            this.password = password.toCharArray()
        }

        try {
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT_MESSAGES", "MQTT CONNECTED!")

                    if (invite != null) {
                        handleRunReturn(invite, mqttClient!!)
                    } else {
                        subscribeOwnerMQTT()
                    }

                    notifyListeners {
                        onNetworkStatusChange(true)
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("MQTT_MESSAGES", "Failed to connect to MQTT: ${exception?.message}")
                    reconnectWithBackoff()
                }
            })

            mqttClient?.setCallback(object : MqttCallback {

                override fun connectionLost(cause: Throwable?) {
                    Log.d("MQTT_MESSAGES", "MQTT DISCONNECTED! $cause ${cause?.message}")

                    notifyListeners {
                        onNetworkStatusChange(false)
                    }

                    reconnectWithBackoff()
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Handle incoming messages here
                    Log.d("MQTT_MESSAGES", "messageArrived: $message")
                    Log.d("MQTT_MESSAGES", "toppicArrived: $topic")

                    if (topic?.contains("/ping") == true) {

                        notifyListeners {
                            listenToOwnerCreation {
                                Log.d("MQTT_MESSAGES", "OWNER EXIST!!!!!!!!!")
                                handleMessageArrived(topic, message)
                            }
                        }

                    } else {
                        handleMessageArrived(topic, message)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Handle message delivery confirmation here
                }
            })
        } catch (e: MqttException) {
            Log.d("MQTT_MESSAGES", "MQTT DISCONNECTED! exception")
            e.printStackTrace()

            notifyListeners {
                onNetworkStatusChange(false)
            }
        }
    }

    private fun handleMessageArrived(topic: String?, message: MqttMessage?) {
        if (topic != null && message?.payload != null) {

            val runReturn = handle(
                topic,
                message.payload,
                ownerSeed ?: "",
                getTimestampInMilliseconds(),
                getCurrentUserState(),
                ownerInfoStateFlow.value?.alias ?: "",
                ownerInfoStateFlow.value?.picture ?: ""
            )

            mqttClient?.let { client ->
                handleRunReturn(runReturn, client)
            }

            Log.d("MQTT_MESSAGES", " this is handle ${runReturn}")
        }

    }

    private fun subscribeOwnerMQTT() {
        try {
            mqttClient?.let { client ->
                // Network setup and handling
                val networkSetup = setNetwork(network)
                handleRunReturn(networkSetup, client)

                // Block height setup and handling
                val blockSetup = setBlockheight(0.toUInt())
                handleRunReturn(blockSetup, client)

                // Subscribe to MQTT topic
                val subtopic = getSubscriptionTopic(
                    ownerSeed!!,
                    getTimestampInMilliseconds(),
                    getCurrentUserState(),
                )

                val qos = IntArray(1) { 1 }
                client.subscribe(arrayOf(subtopic), qos)

                // Subscribe to tribe management topic
                val tribeSubtopic = getTribeManagementTopic(
                    ownerSeed!!,
                    getTimestampInMilliseconds(),
                    getCurrentUserState()
                )
                client.subscribe(arrayOf(tribeSubtopic), qos)

                // Initial setup and handling
                val setUp = initialSetup(
                    ownerSeed!!,
                    getTimestampInMilliseconds(),
                    getCurrentUserState()
                )
                handleRunReturn(setUp, client)

//                if (mnemonicWords != null) {

//                    val fetchMsgBatch = fetchMsgsBatchOkkey(
//                        ownerSeed!!,
//                        getTimestampInMilliseconds(),
//                        getCurrentUserState(),
//                        0.toULong(),
//                        50.toUInt(),
//                        false,
//                        true
//                    )
//                    handleRunReturn(fetchMsgBatch, mqttClient!!)
                }

//                val fetchMessages = fetchMsgs(
//                    ownerSeed!!,
//                    getTimestampInMilliseconds(),
//                    getCurrentUserState(),
//                    ownerInfoStateFlow.value?.messageLastIndex?.plus(1)?.toULong() ?: 0.toULong(),
//                    100.toUInt()
//                )
//
//                handleRunReturn(fetchMessages, client)
//
//                if (inviterContact != null) {
//                    createContact(inviterContact!!)
//                    inviterContact = null
//                }
//            }
        } catch (e: Exception) {
            Log.e("MQTT_MESSAGES", "${e.message}")
        }
    }

    override fun sendMessage(
        sphinxMessage: String,
        contactPubKey: String,
        provisionalId: Long,
        messageType: Int,
        amount: Long?,
        isTribe: Boolean
    ) {
        coroutineScope.launch {

            val now = getTimestampInMilliseconds()

            // Have to include al least 1 sat for tribe messages
            val nnAmount = when {
                isTribe && (amount == null || amount == 0L) -> 1L
                isTribe -> amount ?: 1L
                else -> amount ?: 0L
            }
            try {
                val message = send(
                    ownerSeed!!,
                    now,
                    contactPubKey,
                    messageType.toUByte(),
                    sphinxMessage,
                    getCurrentUserState(),
                    ownerInfoStateFlow.value?.alias ?: "",
                    ownerInfoStateFlow.value?.picture ?: "",
                    convertSatsToMillisats(nnAmount),
                    isTribe
                )
                handleRunReturn(message, mqttClient!!)

                message.msgs.firstOrNull()?.uuid?.let { msgUUID ->
                    notifyListeners {
                        onMessageUUID(msgUUID, provisionalId)
                    }
                }

            } catch (e: Exception) {
                Log.e("MQTT_MESSAGES", "send ${e.message}")
            }
        }
    }

    override fun deleteMessage(
        sphinxMessage: String,
        contactPubKey: String,
        isTribe: Boolean
    ) {
        coroutineScope.launch {
            val now = getTimestampInMilliseconds()

            // Have to include al least 1 sat for tribe messages
            val nnAmount = if (isTribe) 1L else 0L

            try {
                val message = send(
                    ownerSeed!!,
                    now,
                    contactPubKey,
                    MessageType.DELETE.toUByte(),
                    sphinxMessage,
                    getCurrentUserState(),
                    ownerInfoStateFlow.value?.alias ?: "",
                    ownerInfoStateFlow.value?.picture ?: "",
                    convertSatsToMillisats(nnAmount),
                    isTribe
                )
                handleRunReturn(message, mqttClient!!)

            } catch (e: Exception) {
                Log.e("MQTT_MESSAGES", "send ${e.message}")
            }
        }
    }

    override fun joinToTribe(
        tribeHost: String,
        tribePubKey: String,
        tribeRouteHint: String,
        isPrivate: Boolean
    ) {
        coroutineScope.launch {

            val now = getTimestampInMilliseconds()

            try {
                val joinTribeMessage = joinTribe(
                    ownerSeed!!,
                    now,
                    getCurrentUserState(),
                    tribePubKey,
                    tribeRouteHint,
                    ownerInfoStateFlow.value?.alias ?: "",
                    1000.toULong(),
                    isPrivate
                )
                handleRunReturn(joinTribeMessage, mqttClient!!)

            } catch (e: Exception) {
                Log.e("MQTT_MESSAGES", "joinTribe ${e.message}")
            }
        }
    }

    override fun createTribe(tribeServerPubKey: String, tribeJson: String) {
        val now = getTimestampInMilliseconds()

        try {
           val createTribe = uniffi.sphinxrs.createTribe(
                ownerSeed!!,
                now,
                getCurrentUserState(),
                tribeServerPubKey,
                tribeJson
            )
            handleRunReturn(createTribe, mqttClient!!)
        }
        catch (e: Exception) {
            Log.e("MQTT_MESSAGES", "createTribe ${e.message}")
        }
    }

    override fun createInvite(
        nickname: String,
        welcomeMessage: String,
        sats: Long,
        tribeServerPubKey: String?
    ): Pair<String, String>? {
        val now = getTimestampInMilliseconds()

        // Needs to implement tribeServerPubKey and tribeHost in the future

        try {
            val createInvite = makeInvite(
                ownerSeed!!,
                now,
                getCurrentUserState(),
                mixerIp!!,
                convertSatsToMillisats(sats),
                ownerInfoStateFlow.value?.alias ?: "",
                null,
                tribeServerPubKey
            )

            if (createInvite.newInvite != null) {
                handleRunReturn(createInvite, mqttClient!!)

                val invite = createInvite.newInvite ?: return null
                val code = codeFromInvite(invite)

                return Pair(invite, code)
            }

        } catch (e: Exception) {
            Log.e("MQTT_MESSAGES", "createInvite ${e.message}")
        }
        return null
    }

    override fun createInvoice(amount: Long, memo: String): Pair<String, String>? {
        val now = getTimestampInMilliseconds()

        try {
            val makeInvoice = uniffi.sphinxrs.makeInvoice(
                ownerSeed!!,
                now,
                getCurrentUserState(),
                convertSatsToMillisats(amount),
                memo
            )
            handleRunReturn(makeInvoice, mqttClient!!)

            val invoice = makeInvoice.invoice

            if (invoice != null) {
                val paymentHash = paymentHashFromInvoice(invoice)
                return Pair(invoice, paymentHash)
            }

        } catch (e: Exception) {
            Log.e("MQTT_MESSAGES", "makeInvoice ${e.message}")
        }
        return null
    }

    override fun processInvoicePayment(paymentRequest: String) {
        val now = getTimestampInMilliseconds()

        try {
            val processInvoice = uniffi.sphinxrs.payContactInvoice(
                ownerSeed!!,
                now,
                getCurrentUserState(),
                paymentRequest,
                ownerInfoStateFlow.value?.alias ?: "",
                ownerInfoStateFlow.value?.picture ?: "",
                false // not implemented on tribes yet
            )
            handleRunReturn(processInvoice, mqttClient!!)
        } catch (e: Exception) {
            Log.e("MQTT_MESSAGES", "processInvoicePayment ${e.message}")
        }
    }

    override fun retrievePaymentHash(paymentRequest: String): String? {
        return try {
            paymentHashFromInvoice(paymentRequest)
        } catch (e: Exception) {
            null
        }
    }

    override fun retrieveTribeMembersList(tribeServerPubKey: String, tribePubKey: String) {
        val now = getTimestampInMilliseconds()

        try {
            val tribeMembers = listTribeMembers(
                ownerSeed!!,
                now,
                getCurrentUserState(),
                tribeServerPubKey,
                tribePubKey
            )
            handleRunReturn(tribeMembers, mqttClient!!)
        }
        catch (e: Exception) {
            Log.e("MQTT_MESSAGES", "tribeMembers ${e.message}")
        }
    }

    override fun fetchContactsOnRestoreAccount() {
        try {
            val fetchContacts = uniffi.sphinxrs.fetchMsgsBatchOkkey(
                ownerSeed!!,
                getTimestampInMilliseconds(),
                getCurrentUserState(),
                0.toULong(),
                50.toUInt(),
                false,
                true
            )
            handleRunReturn(fetchContacts, mqttClient!!)
        } catch (e: Exception) {
            Log.e("MQTT_MESSAGES", "fetchContactsOnRestoreAccount ${e.message}")
        }
    }

    override fun generateMediaToken(
        contactPubKey: String,
        muid: String,
        host: String,
        metaData: String?,
        amount: Long?
    ): String? {
        val now = getTimestampInMilliseconds()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.YEAR, 1)

        val yearFromNow = try {
            (calendar.timeInMillis / 1000).toUInt()
        } catch (e: Exception) {
            null
        }

        return try {
            if (amount != null && amount > 0) {
                makeMediaTokenWithPrice(
                    ownerSeed!!,
                    now,
                    getCurrentUserState(),
                    host,
                    muid,
                    contactPubKey,
                    yearFromNow!!,
                    convertSatsToMillisats(amount),
                )
            } else {
                if (metaData != null) {
                    makeMediaTokenWithMeta(
                        ownerSeed!!,
                        now,
                        getCurrentUserState(),
                        host,
                        muid,
                        contactPubKey,
                        yearFromNow!!,
                        metaData
                    )
                } else {
                    makeMediaToken(
                        ownerSeed!!,
                        now,
                        getCurrentUserState(),
                        host,
                        muid,
                        contactPubKey,
                        yearFromNow!!
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("MQTT_MESSAGES", "Error to generate media token $e")
            null
        }
    }

    private fun publishTopicsSequentially(topics: Array<String>, messages: Array<String>?, index: Int) {
        if (index < topics.size) {
            val topic = topics[index]
            val mqttMessage = messages?.getOrNull(index)

            val message = if (mqttMessage?.isNotEmpty() == true) {
                MqttMessage(mqttMessage.toByteArray())
            } else {
                MqttMessage()
            }

            mqttClient?.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // Recursively call the function with the next index
                    publishTopicsSequentially(topics, messages, index + 1)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("MQTT_MESSAGES", "Failed to publish to $topic: ${exception?.message}")
                }
            })
        }
    }

    private fun handleRunReturn(rr: RunReturn, client: MqttAsyncClient) {
        // Set updated state into db
        rr.stateMp?.let {
            storeUserState(it)
            Log.d("MQTT_MESSAGES", "=> stateMp $it")
        }

        // Publish to topics based on the new array structure
        rr.topics.forEachIndexed { index, topic ->
            val payload = rr.payloads.getOrElse(index) { ByteArray(0) }
            client.publish(topic, MqttMessage(payload))
            Log.d("MQTT_MESSAGES", "=> published to $topic")
        }

        // Set your balance
        rr.newBalance?.let { newBalance ->
            convertMillisatsToSats(newBalance)?.let { balance ->
                notifyListeners {
                    onNewBalance(balance)
                }
            }
            Log.d("MQTT_MESSAGES", "===> BALANCE ${newBalance.toLong()}")
        }

        // Process each message in the new msgs array
        rr.msgs.forEach { msg ->

            // Handling sent messages
            msg.sentTo?.let { sentTo ->
                notifyListeners {
                    onMessageSent(
                        msg.message.orEmpty(),
                        sentTo,
                        msg.type?.toInt() ?: 0,
                        msg.uuid.orEmpty(),
                        msg.index.orEmpty(),
                        msg.timestamp?.toLong()
                    )
                }
                Log.d("MQTT_MESSAGES", "Sent message to $sentTo")
            }

            // Handling received messages
            msg.sender?.let { sender ->
                notifyListeners {

                    onMessageReceived(
                        msg.message.orEmpty(),
                        sender,
                        msg.type?.toInt() ?: 0,
                        msg.uuid.orEmpty(),
                        msg.index.orEmpty(),
                        msg.msat?.let { convertMillisatsToSats(it) },
                        msg.timestamp?.toLong()
                    )
                }
                Log.d("MQTT_MESSAGES", "Received message from $sender")
            }
        }

        // Handling new tribe and tribe members
        rr.newTribe?.let { newTribe ->
            notifyListeners {
                onNewTribe(newTribe)
            }
            Log.d("MQTT_MESSAGES", "===> newTribe $newTribe")
        }

        rr.tribeMembers?.let { tribeMembers ->
            notifyListeners {
                onTribeMembersList(tribeMembers)
            }
            Log.d("MQTT_MESSAGES", "=> tribeMembers $tribeMembers")
        }

        // Handling my contact info
        rr.myContactInfo?.let { myContactInfo ->
            val parts = myContactInfo.split("_", limit = 2)
            val okKey = parts.getOrNull(0)
            val routeHint = parts.getOrNull(1)
            val isRestoreAccount = mnemonicWords != null

            if (okKey != null && routeHint != null) {
                notifyListeners {
                    onOwnerRegistered(okKey, routeHint, isRestoreAccount)
                }
            }
            Log.d("MQTT_MESSAGES", "=> my_contact_info $myContactInfo")
        }

        // Handling new invite created
        rr.newInvite?.let { invite ->
            notifyListeners {
                onNewInviteCreated(invite)
            }
            Log.d("MQTT_MESSAGES", "=> new_invite $invite")
        }

        rr.inviterContactInfo?.let { inviterInfo ->
            val parts = inviterInfo.split("_", limit = 2)
            val okKey = parts.getOrNull(0)?.toLightningNodePubKey()
            val routeHint = parts.getOrNull(1)?.toLightningRouteHint()

            val code = codeFromInvite(inviteCode!!)

            // Ensure the owner is subscribed before set the inviter contact
            // The inviter will be added after the owner sets its alias and first init the dashboard
            subscribeOwnerMQTT()

            inviterContact = NewContact(
                null,
                okKey,
                routeHint,
                null,
                false,
                null,
                code,
                null
            )

            Log.d("MQTT_MESSAGES", "=> inviterInfo $inviterInfo")
        }

        rr.initialTribe?.let { initialTribe ->
            // Call joinTribe with the url that comes on initialTribe

            Log.d("MQTT_MESSAGES", "=> initialTribe $initialTribe")
        }

        rr.stateToDelete.let {
            notifyListeners {
                onDeleteUserState(it)
            }

            Log.d("MQTT_MESSAGES", "=> stateToDelete $it")
        }

        // Handling other properties like sentStatus, settledStatus, error, etc.
        rr.error?.let { error ->
            Log.d("MQTT_MESSAGES", "=> error $error")
        }

        // Sent
        rr.sentStatus?.let { sentStatus ->
            Log.d("MQTT_MESSAGES", "=> sent_status $sentStatus")
        }

        // Settled
        rr.settledStatus?.let { settledStatus ->
            Log.d("MQTT_MESSAGES", "=> settled_status $settledStatus")
        }

        rr.lspHost?.let { lspHost ->
            mixerIp = lspHost
        }

    }
    override fun retrieveLspIp(): String? {
        return mixerIp
    }

    override fun processChallengeSignature(challenge: String) {

        val signedChallenge = try {
            signBytes(
                ownerSeed!!,
                0.toULong(),
                getTimestampInMilliseconds(),
                network,
                challenge.toByteArray()
            )
        } catch (e: Exception) {
            null
        }

        if (signedChallenge != null) {

           val sign = ByteArray(signedChallenge.length / 2) { index ->
               val start = index * 2
               val end = start + 2
               val byteValue = signedChallenge.substring(start, end).toInt(16)
               byteValue.toByte()
           }.encodeBase64()
               .replace("/", "_")
               .replace("+", "-")

            notifyListeners {
                onSignedChallenge(sign)
            }
        }
    }

    // Utility Methods

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun generateRandomBytes(size: Int): UByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        val uByteArray = UByteArray(size)

        for (i in bytes.indices) {
            uByteArray[i] = bytes[i].toUByte()
        }

        return uByteArray
    }

    private fun getTimestampInMilliseconds(): String =
        System.currentTimeMillis().toString()

    private fun resetMQTT() {
        if (mqttClient?.isConnected == true) {
            mqttClient?.disconnect()
        }
        mqttClient = null
    }


    private val synchronizedListeners = SynchronizedListenerHolder()

    override fun addListener(listener: ConnectManagerListener): Boolean {
        return synchronizedListeners.addListener(listener)
    }

    override fun removeListener(listener: ConnectManagerListener): Boolean {
        return synchronizedListeners.removeListener(listener)
    }

    private fun notifyListeners(action: ConnectManagerListener.() -> Unit) {
        synchronizedListeners.forEachListener { listener ->
            action(listener)
        }
    }

    private inner class SynchronizedListenerHolder {
        private val listeners: LinkedHashSet<ConnectManagerListener> = LinkedHashSet()

        fun addListener(listener: ConnectManagerListener): Boolean = synchronized(this) {
            listeners.add(listener).also {
                if (it) {
                    // Log listener registration
                }
            }
        }

        fun removeListener(listener: ConnectManagerListener): Boolean = synchronized(this) {
            listeners.remove(listener).also {
                if (it) {
                    // Log listener removal
                }
            }
        }

        fun forEachListener(action: (ConnectManagerListener) -> Unit) {
            synchronized(this) {
                listeners.forEach(action)
            }
        }
    }

    private fun storeUserState(state: ByteArray) {
        try {
            val decoded = MsgPack.decodeFromByteArray(MsgPackDynamicSerializer, state)
            (decoded as? MutableMap<String, ByteArray>)?.let {
                storeUserStateOnSharedPreferences(it)
            }

        } catch (e: Exception) { }
    }

    private fun storeUserStateOnSharedPreferences(newUserState: MutableMap<String, ByteArray>) {
        val existingUserState = retrieveUserStateMap(ownerInfoStateFlow.value?.userState)
        existingUserState.putAll(newUserState)

        val encodedString = encodeMapToBase64(existingUserState)

        // Update class var
        _ownerInfoStateFlow.value = ownerInfoStateFlow.value?.copy(
            userState = encodedString
        )

        // Update SharedPreferences
        notifyListeners {
            onUpdateUserState(encodedString)
        }
    }

    private fun retrieveUserStateMap(encodedString: String?): MutableMap<String, ByteArray> {
        val result = encodedString?.let {
            decodeBase64ToMap(it)
        } ?: mutableMapOf()

        return result
    }

    private fun getCurrentUserState(): ByteArray {
        val userStateMap = retrieveUserStateMap(ownerInfoStateFlow.value?.userState)
        return MsgPack.encodeToByteArray(MsgPackDynamicSerializer, userStateMap)
    }

    private fun encodeMapToBase64(map: MutableMap<String, ByteArray>): String {
        val encodedMap = mutableMapOf<String, String>()

        for ((key, value) in map) {
            encodedMap[key] = Base64.encodeToString(value, Base64.NO_WRAP)
        }

        val result = (encodedMap as Map<*, *>?)?.let { JSONObject(it).toString() } ?: ""


        return result
    }

    private fun decodeBase64ToMap(encodedString: String): MutableMap<String, ByteArray> {
        if (encodedString.isEmpty()) {
            return mutableMapOf()
        }

        val decodedMap = mutableMapOf<String, ByteArray>()

        try {
            val jsonObject = JSONObject(encodedString)
            val keys = jsonObject.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                val encodedValue = jsonObject.getString(key)
                val decodedValue = Base64.decode(encodedValue, Base64.NO_WRAP)
                decodedMap[key] = decodedValue
            }
        } catch (e: JSONException) { }

        return decodedMap
    }
    private var reconnectAttempts = 0
    private val initialReconnectDelay = 1000L
    private val maxReconnectDelay = 10000L
    private val backoffMultiplier = 2.0

    private fun reconnectWithBackoff() {
        resetMQTT()
        coroutineScope.launch {
            val delayTime = calculateBackoffDelay()
            delay(delayTime)

            if (!isConnected()) {
                initializeMqttAndSubscribe(
                    mixerIp!!,
                    walletMnemonic!!,
                    ownerInfoStateFlow.value!!,
                )
                Log.d("MQTT_MESSAGES",  "onReconnectMqtt" )
            }

            delay(1000)

            if (!isConnected()) {
                reconnectWithBackoff()
                Log.d("MQTT_MESSAGES",  "reconnectWithBackoff" )
            } else {
                reconnectAttempts = 0
            }
        }
    }

    private fun convertSatsToMillisats(sats: Long): ULong {
        return (sats * 1_000).toULong()
    }

    fun convertMillisatsToSats(millisats: ULong): Long? {
        try {
            return (millisats / 1_000uL).toLong()
        } catch (e: Exception) {
            return null
        }
    }

    private fun calculateBackoffDelay(): Long {
        val delay = initialReconnectDelay * backoffMultiplier.pow(reconnectAttempts.toDouble()).toLong()
        reconnectAttempts++
        return min(delay, maxReconnectDelay) // Ensure the delay does not exceed the maximum
    }

    private fun isConnected(): Boolean {
        return mqttClient?.isConnected ?: false
    }

}