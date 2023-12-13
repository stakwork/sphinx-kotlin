package chat.sphinx.feature_connect_manager

import android.util.Log
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.example.concept_connect_manager.ConnectManagerListener
import chat.sphinx.example.concept_connect_manager.model.TopicHandler
import chat.sphinx.example.concept_connect_manager.model.ConnectionState
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_contact.NewContact
import chat.sphinx.wrapper_common.lightning.retrieveLightningRouteHint
import chat.sphinx.wrapper_contact.ContactInfo
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject
import uniffi.sphinxrs.createOnionMsg
import uniffi.sphinxrs.mnemonicFromEntropy
import uniffi.sphinxrs.mnemonicToSeed
import uniffi.sphinxrs.peelOnionMsg
import uniffi.sphinxrs.pubkeyFromSeed
import uniffi.sphinxrs.rootSignMs
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
        get() = _connectionStateStateFlow

    companion object {
        const val KEY_EXCHANGE = 10
        const val KEY_EXCHANGE_CONFIRMATION = 11
    }

    // Key Generation and Management

    override fun createAccount() {
        coroutineScope.launch {

            val seed = generateMnemonic()

            val xPub = seed.first?.let {
                generateXPub(
                    it,
                    getTimestampInMilliseconds(),
                    network
                )
            }

            val okKey = seed.first?.let {
                generatePubKeyFromSeed(
                    it,
                    0.toUInt(),
                    getTimestampInMilliseconds(),
                    network
                )
            }

            val now = getTimestampInMilliseconds()

            val sig = seed.first?.let {
                rootSignMs(
                    it,
                    now,
                    network
                )
            }

            val serverURI = mixer

            if (xPub != null && sig != null && okKey != null && serverURI != null) {

                ownerSeed = seed.first

                notifyListeners {
                    onOkKey(okKey)
                }

                connectToMQTT(
                    serverURI,
                    xPub,
                    now,
                    sig,
                    okKey,
                    null
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
        index: UInt,
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

            val childPubKey = ownerSeed?.let {
                generatePubKeyFromSeed(
                    it,
                    contact.index.value.toUInt(),
                    now,
                    network
                )
            }

            val index = contact.index

            if (childPubKey != null) {

                subscribeAndPublishContactMQTT(
                    childPubKey,
                    index.value.toInt()
                )
            }
        }
    }

    // MQTT Connection Management

    override fun initializeMqttAndSubscribe(
        serverUri: String,
        mnemonicWords: WalletMnemonic,
        okKey: String,
        contacts: List<ContactInfo>?,
        lspPubKey: LightningNodePubKey?
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
                ownerLspPubKey = lspPubKey

                connectToMQTT(
                    serverUri,
                    xPub,
                    now,
                    sig,
                    okKey,
                    contacts
                )
            }
        }
    }

    private fun connectToMQTT(
        serverURI: String,
        clientId: String,
        key: String,
        password: String,
        okKey: String,
        contacts: List<ContactInfo>?
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
                    subscribeOwnerMQTT(okKey)

                    contacts?.let {
                        subscribeContacts(it)
                    }
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

                    handleMqttMessage(topic, message)
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

    private fun subscribeOwnerMQTT(okKey: String){
        // Balance is causing a error because is not implemented yet.
        // The body message belongs to registerMsg

        val topics = arrayOf("${okKey}/${0}/res/#")
        val qos = IntArray(topics.size) { 1 }

        mqttClient?.subscribe(topics, qos)

    //  val balance = "${okKey}/${0}/req/balance"
        val registerOkKey = "${okKey}/${0}/req/register"
        val registerMsg = "${okKey}/${0}/req/msgs"


        val body = """
        {
            "since": 0,
            "limit": 1000
        }
        """.trimIndent()

        val mqttmessages = arrayOf("", body)
        val topicsArray = arrayOf(registerOkKey, registerMsg)

        publishTopicsSequentially(topicsArray, mqttmessages,0)
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
        hops: String,
        okKey: String
    ) {
        coroutineScope.launch {

            val now = getTimestampInMilliseconds()

            val seed = ownerSeed

            if (seed != null) {

                val onion = try {
                    createOnionMsg(
                        seed,
                        0.toUInt(),
                        now,
                        network,
                        hops,
                        sphinxMessage
                    )
                } catch (e: Exception) {
                    null
                }

                if (onion != null && mqttClient?.isConnected == true) {
                    Log.d("ONION_PROCESS", "The Onion is contain\n $sphinxMessage")

                    val publishTopic = "${okKey}/${0}/req/send"

                    try {
                        mqttClient?.publish(publishTopic, MqttMessage(onion))
                    } catch (e: MqttException) {
                        e.printStackTrace()
                    }
                }
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
                        onOwnerRegistered(connectionState.message)
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

            notifyListeners {
                onNewContactRegistered(
                    connectionState.index,
                    childKey,
                    scid,
                    generatedContactRouteHint.value
                )
            }
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
                        0.toUInt(),
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

}