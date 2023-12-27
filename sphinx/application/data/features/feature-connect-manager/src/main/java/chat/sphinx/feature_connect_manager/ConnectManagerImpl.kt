package chat.sphinx.feature_connect_manager

import android.util.Base64
import android.util.Log
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.example.concept_connect_manager.ConnectManagerListener
import chat.sphinx.example.concept_connect_manager.model.TopicHandler
import chat.sphinx.example.concept_connect_manager.model.ConnectionState
import chat.sphinx.example.concept_connect_manager.model.OwnerInfo
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_contact.NewContact
import chat.sphinx.wrapper_common.lightning.retrieveLightningRouteHint
import chat.sphinx.wrapper_contact.ContactInfo
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPackDynamicSerializer
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
import uniffi.sphinxrs.createOnionMsg
import uniffi.sphinxrs.fetchMsgs
import uniffi.sphinxrs.getSubscriptionTopic
import uniffi.sphinxrs.handle
import uniffi.sphinxrs.initialSetup
import uniffi.sphinxrs.mnemonicFromEntropy
import uniffi.sphinxrs.mnemonicToSeed
import uniffi.sphinxrs.peelOnionMsg
import uniffi.sphinxrs.pubkeyFromSeed
import uniffi.sphinxrs.rootSignMs
import uniffi.sphinxrs.send
import uniffi.sphinxrs.setBlockheight
import uniffi.sphinxrs.setNetwork
import uniffi.sphinxrs.xpubFromSeed
import java.security.SecureRandom

class ConnectManagerImpl(
    dispatchers: CoroutineDispatchers
): ConnectManager(),
    CoroutineDispatchers by dispatchers
{
    private var mixer: String? = null
    private var walletMnemonic: WalletMnemonic? = null
    private var mqttClient: MqttAsyncClient? = null
    private val network = "regtest"
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ownerSeed: String? = null
    private var ownerLspPubKey: LightningNodePubKey? = null

    private val _connectionStateStateFlow = MutableStateFlow<ConnectionState?>(null)
    override val connectionStateStateFlow: StateFlow<ConnectionState?>
        get() = _connectionStateStateFlow.asStateFlow()

    private val _ownerInfoStateFlow: MutableStateFlow<OwnerInfo?> by lazy {
        MutableStateFlow(null)
    }

    override val ownerInfoStateFlow: StateFlow<OwnerInfo?>
        get() = _ownerInfoStateFlow.asStateFlow()

    companion object {
        const val KEY_EXCHANGE = 10
        const val KEY_EXCHANGE_CONFIRMATION = 11
        const val TEXT_MESSAGE = 0
    }

    // Key Generation and Management

    override fun createAccount() {
        coroutineScope.launch {

            val seed = generateMnemonic()
            val now = getTimestampInMilliseconds()

            val serverURI = mixer

            val xPub = seed.first?.let {
                generateXPub(
                    it,
                    getTimestampInMilliseconds(),
                    network
                )
            }

            val sig = seed.first?.let {
                rootSignMs(
                    it,
                    now,
                    network
                )
            }

            if (xPub != null && sig != null && serverURI != null) {

                ownerSeed = seed.first

                connectToMQTT(
                    serverURI,
                    xPub,
                    now,
                    sig
                )
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun generateMnemonic(): Pair<String?, WalletMnemonic?> {
        var seed: String? = null

        walletMnemonic = try {
            val randomBytes = generateRandomBytes(16)
            val randomBytesString =
                randomBytes.joinToString("") { it.toString(16).padStart(2, '0') }
            val words = mnemonicFromEntropy(randomBytesString)

            words.toWalletMnemonic()
        } catch (e: Exception) {
            null
        }

        walletMnemonic?.value?.let { words ->
            try {
                seed = mnemonicToSeed(words)

                notifyListeners {
                    onMnemonicWords(words)
                }

            } catch (e: Exception) {}
        }

        return Pair(seed, walletMnemonic)
    }

    private fun generateXPub(seed: String, time: String, network: String): String? {
        return try {
            xpubFromSeed(seed, time, network)
        } catch (e: Exception) {
            null
        }
    }

    private fun generatePubKeyFromSeed(
        seed: String,
        index: ULong,
        time: String,
        network: String
    ): String? {
        return try {
            pubkeyFromSeed(seed, index, time, network)
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
                    3000.toULong()
                )

                handleRunReturn(
                    runReturn,
                    mqttClient!!
                )
            } catch (e: Exception) {
                Log.e("MQTT_MESSAGES", "add contact excp $e")
            }


//            val childPubKey = ownerSeed?.let {
//                generatePubKeyFromSeed(
//                    it,
//                    contact.index.value.toULong(),
//                    now,
//                    network
//                )
//            }
//
//            val index = contact.index
//
//            if (childPubKey != null) {
//
//                subscribeAndPublishContactMQTT(
//                    childPubKey,
//                    index.value.toInt()
//                )
//            }
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

                    subscribeOwnerMQTT()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("MQTT_MESSAGES", "Failed to connect to MQTT: ${exception?.message}")
                    // Handle connection failure here
                }
            })

            mqttClient?.setCallback(object : MqttCallback {

                override fun connectionLost(cause: Throwable?) {
                    Log.d("MQTT_MESSAGES", "MQTT DISCONNECTED! $cause ${cause?.message}")
                    // Implement reconnection logic here
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Handle incoming messages here
                    Log.d("MQTT_MESSAGES", "$topic")
                    Log.d("MQTT_MESSAGES", "$message")
                    Log.d("MQTT_MESSAGES", "${message?.payload}")

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

//                    handleMqttMessage(topic, message)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Handle message delivery confirmation here
                }
            })
        } catch (e: MqttException) {
            Log.d("MQTT_MESSAGES", "MQTT DISCONNECTED! exception")
            e.printStackTrace()
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

                // Initial setup and handling
                val setUp = initialSetup(
                    ownerSeed!!,
                    getTimestampInMilliseconds(),
                    getCurrentUserState()
                )
                handleRunReturn(setUp, client)

               val fetchMessages =  fetchMsgs(
                    ownerSeed!!,
                    getTimestampInMilliseconds(),
                    getCurrentUserState(),
                    0.toULong(),
                    100.toUInt()
                )

                handleRunReturn(fetchMessages, client);
            }
        } catch (e: Exception) {
            Log.e("MQTT_MESSAGES", "${e.message}")
        }
    }

    private fun subscribeContacts(contacts: List<ContactInfo>) {
        val subscribeTopic = contacts.map { contactInfo ->
            "${contactInfo.childPubKey.value}/${contactInfo.contactIndex.value}/res/#"
        }.toTypedArray()

        val publishTopic = contacts.map { contactInfo ->
            "${contactInfo.childPubKey.value}/${contactInfo.contactIndex.value}/req/register"
        }.toTypedArray()

        val messages = contacts.map { contactInfo ->
            contactInfo.messagesFetchRequest
        }.toTypedArray()

        val qos = IntArray(subscribeTopic.size) { 1 }

        mqttClient?.subscribe(subscribeTopic, qos)

        publishTopicsSequentially(publishTopic, messages, 0)
    }

    private fun subscribeAndPublishContactMQTT(
        childPubKey: String,
        index: Int,
    ) {
        if (mqttClient?.isConnected == true) {
            coroutineScope.launch {

                val subscribeTopic = "${childPubKey}/${index}/res/#"
                val publishTopic = "${childPubKey}/${index}/req/register"

                try {
                    mqttClient?.subscribe(subscribeTopic, 1)
                    mqttClient?.publish(publishTopic, MqttMessage())
                } catch (e: MqttException) {
                    e.printStackTrace()
                }
            }
        } else {
            Log.d("MQTT_MESSAGES", "MQTT Client is not connected.")
        }
    }

    override fun sendMessage(
        sphinxMessage: String,
        contactPubKey: String
    ) {
        coroutineScope.launch {

            val now = getTimestampInMilliseconds()

            try {
                val message = send(
                    ownerSeed!!,
                    now,
                    contactPubKey,
                    0.toUByte(),
                    sphinxMessage,
                    getCurrentUserState(),
                    ownerInfoStateFlow.value?.alias ?: "",
                    ownerInfoStateFlow.value?.picture ?: "",
                    0.toULong()
                )
                handleRunReturn(message, mqttClient!!)

            } catch (e: Exception) {
                Log.e("MQTT_MESSAGES", "send ${e.message}")
            }
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

    private fun handleMqttMessage(topic: String?, message: MqttMessage?) {
        topic?.let { nnTopic ->
            val connectionState = TopicHandler.retrieveConnectionState(nnTopic, message.toString(), message?.payload)

            when (connectionState) {
                is ConnectionState.OwnerRegistered -> {
                    notifyListeners {
//                        onOwnerRegistered(connectionState.message,)
                    }
                }
                is ConnectionState.ContactRegistered -> {
                    handleContactRegistered(nnTopic, connectionState)
                }
                is ConnectionState.OnionMessage -> {
                    handleOnionMessage(connectionState.payload)
                }
                else -> {
                    _connectionStateStateFlow.value = connectionState
                }
            }
        }
    }

    private fun handleRunReturn(rr: RunReturn, client: MqttAsyncClient) {
        // Set updated state into db
        rr.stateMp?.let {
            storeUserState(it)
            Log.d("MQTT_MESSAGES", "=> stateMp $it")
        }

        // Publish to topic 0
        rr.topic0?.let { topic ->
            Log.d("MQTT_MESSAGES", "=> topic_0 $topic")
            val pld = rr.payload0 ?: ByteArray(0)
            client.publish(topic, MqttMessage(pld))
        }

        // Publish to topic 1
        rr.topic1?.let { topic ->
            Log.d("MQTT_MESSAGES", "=> topic_1 $topic")
            val pld = rr.payload1 ?: ByteArray(0)
            client.publish(topic, MqttMessage(pld))
        }

        // Publish to topic 2
        rr.topic2?.let { topic ->
            Log.d("MQTT_MESSAGES", "=> topic_2 $topic")
            val pld = rr.payload2 ?: ByteArray(0)
            client.publish(topic, MqttMessage(pld))
        }

        // Set your balance
        rr.newBalance?.let { newBalance ->
            Log.d("MQTT_MESSAGES", "===> BALANCE ${newBalance.toLong()}")
            // BALANCE = newBalance.toLong()
        }

        // Incoming message json
        rr.msg?.let { msg ->
            Log.d("MQTT_MESSAGES", "=> received msg $msg, ${rr.msgUuid}, ${rr.msgIndex}")
        }

        // Incoming sender info json
        rr.msgSender?.let { msgSender ->
            try {
                val jsonObject = JSONObject(msgSender)

                val pubkey = jsonObject.getString("pubkey")
                val alias = jsonObject.getString("alias")
                val photoUrl = jsonObject.optString("photo_url")
                val person = jsonObject.optString("person")
                val confirmed = jsonObject.getBoolean("confirmed")

                notifyListeners {
                    onNewContactRegistered(
                        pubkey,
                        alias,
                        photoUrl,
                        confirmed
                    )
                }
            } catch (e: Exception){}

            Log.d("MQTT_MESSAGES", "=> received msg_sender $msgSender")
        }

        // Print my contact info
        rr.myContactInfo?.let { myContactInfo ->
            Log.d("MQTT_MESSAGES", "=> my_contact_info $myContactInfo")
            val parts = myContactInfo.split("_", limit = 2)

            val okKey = parts.getOrNull(0)
            val routeHint = parts.getOrNull(1)

            if (okKey != null && routeHint != null) {
                notifyListeners {
                    onOwnerRegistered(okKey, routeHint)
                }
            }
        }

        // Sent
        rr.sentStatus?.let { sentStatus ->
            Log.d("MQTT_MESSAGES", "=> sent_status $sentStatus")
        }

        // Settled
        rr.settledStatus?.let { settledStatus ->
            Log.d("MQTT_MESSAGES", "=> settled_status $settledStatus")
        }

        // Incoming error string
        rr.error?.let { error ->
            Log.d("MQTT_MESSAGES", "=> error $error")
        }
    }

    private fun handleContactRegistered(
        topic: String,
        connectionState: ConnectionState.ContactRegistered
    ) {

        val jsonObject = try {
            JSONObject(connectionState.message)
        } catch (e: Exception) {
            null
        }

        val scid = jsonObject?.getString("scid") ?: return
        val generatedContactRouteHint = retrieveLightningRouteHint(ownerLspPubKey?.value, scid) ?: return

        connectionState.childPubKey?.let { childKey ->

//            notifyListeners {
//                onNewContactRegistered(
//                    connectionState.index,
//                    childKey,
//                    scid
//                )
//            }
        }
    }

    private fun handleOnionMessage(payload: ByteArray?) {
        payload ?: return

        coroutineScope.launch {
            val now = getTimestampInMilliseconds()

            ownerSeed?.let { seed ->

                val decryptedJson = try {
                    peelOnionMsg(
                        seed,
                        0.toULong(),
                        now,
                        network,
                        payload
                    )
                } catch (e: Exception) {
                    Log.d("ONION_PROCESS", "This is the PeelOnionMsg EXCEPTION\n ${e.message}")
                }

                val jsonObject = try {
                    JSONObject(decryptedJson.toString())
                } catch (e: Exception) {
                    null
                }
                val messageType = jsonObject?.getInt("type")

                when (messageType) {
                    KEY_EXCHANGE -> {
                        notifyListeners {
                            onKeyExchange(decryptedJson.toString())
                        }
                    }
                    KEY_EXCHANGE_CONFIRMATION -> {
                        notifyListeners {
                            onKeyExchangeConfirmation(decryptedJson.toString())
                        }
                    }
                    TEXT_MESSAGE -> {
                        notifyListeners {
                            onTextMessageReceived(decryptedJson.toString())
                        }
                    }
                    else -> {}
                }

                Log.d("ONION_PROCESS", "This is the PeelOnionMsg\n $decryptedJson")
            }
        }
    }

    override fun setLspIp(ip: String) {
        mixer = ip
    }

    override fun retrieveLspIp(): String? {
        return mixer
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
                Log.e("MSGPACK", "Dashboard storeUserState $it")
            }

        } catch (e: Exception) { }
    }

    private fun storeUserStateOnSharedPreferences(newUserState: MutableMap<String, ByteArray>) {
        val existingUserState = retrieveUserStateMap(ownerInfoStateFlow.value?.userState)
        existingUserState.putAll(newUserState)
        Log.e("MSGPACK", "Dashboard $existingUserState")

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

        Log.e("MSGPACK", "dasboard encodeMapToBase64 $result")


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
}