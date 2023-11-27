package chat.sphinx.feature_connect_manager

import android.util.Log
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.example.concept_connect_manager.model.TopicHandler
import chat.sphinx.example.concept_connect_manager.model.ConnectionState
import chat.sphinx.wrapper_contact.NewContact
import chat.sphinx.wrapper_common.contact.toContactIndex
import chat.sphinx.wrapper_common.lightning.retrieveLightningRouteHint
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningRouteHint
import chat.sphinx.wrapper_common.lightning.toShortChannelId
import chat.sphinx.wrapper_contact.toContactAlias
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import uniffi.sphinxrs.createOnionMsg
import uniffi.sphinxrs.mnemonicFromEntropy
import uniffi.sphinxrs.mnemonicToSeed
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
    private var newContact: NewContact? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val topicHandlers: Map<String, TopicHandler> = setupTopicHandlers()

    private val _connectionStateStateFlow = MutableStateFlow<ConnectionState?>(null)
    override val connectionStateStateFlow: StateFlow<ConnectionState?>
        get() = _connectionStateStateFlow

    // Core Functional Methods


    override fun initializeMqttAndSubscribe(
        serverUri: String,
        mnemonicWords: WalletMnemonic,
        okKey: String,
        contacts: HashMap<String, Int>
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

    override fun createAccount() {
        coroutineScope.launch {

            val seed = generateMnemonic(null)

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

                _connectionStateStateFlow.value = ConnectionState.OkKey(okKey)

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

    override fun createContact(
        alias: String,
        lightningNodePubKey: String,
        lightningRouteHint: String,
        index: Long,
        walletMnemonic: WalletMnemonic,
        senderLspPubKey: String
    ) {
        coroutineScope.launch {

            val seed = try {
                mnemonicToSeed(walletMnemonic.value)
            } catch (e: Exception) {
                null
            }

            val now = getTimestampInMilliseconds()

            val childPubKey = seed?.let {
                generatePubKeyFromSeed(
                    it,
                    index.toUInt(),
                    now,
                    network
                )
            }

            val index = index.toContactIndex()

            if (childPubKey != null && index != null) {

                newContact = NewContact(
                    contactAlias = alias.toContactAlias(),
                    lightningNodePubKey = lightningNodePubKey.toLightningNodePubKey(),
                    lightningRouteHint = lightningRouteHint.toLightningRouteHint(),
                    index = index,
                    childPubKey = childPubKey.toLightningNodePubKey(),
                    contactRouteHint = null ,
                    scid = null,
                    senderLspPubKey = senderLspPubKey.toLightningNodePubKey()
                )

                subscribeAndPublishContactMQTT(
                    childPubKey,
                    index.value.toInt()
                )
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun generateMnemonic(mnemonicWords: String?): Pair<String?, WalletMnemonic?> {
        var seed: String? = null

        walletMnemonic = run {
            try {
                mnemonicWords?.toWalletMnemonic() ?: run {
                    val randomBytes = generateRandomBytes(16)
                    val randomBytesString =
                        randomBytes.joinToString("") { it.toString(16).padStart(2, '0') }
                    val words = mnemonicFromEntropy(randomBytesString)

                    words.toWalletMnemonic()
                }
            } catch (e: Exception) {
                val excep = e
                null
            }
        }

        walletMnemonic?.value?.let { words ->
            try {
                seed = mnemonicToSeed(words)
                _connectionStateStateFlow.value = ConnectionState.MnemonicWords(words)
            } catch (e: Exception) {}
        }

        return Pair(seed, walletMnemonic)
    }

    override suspend fun generateXPub(seed: String, time: String, network: String): String? {
        return try {
            xpubFromSeed(seed, time, network)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun generatePubKeyFromSeed(
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

    private fun connectToMQTT(
        serverURI: String,
        clientId: String,
        key: String,
        password: String,
        okKey: String,
        contacts: HashMap<String, Int>?
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
        val topics = arrayOf("${okKey}/${0}/res/#")
        val qos = IntArray(topics.size) { 1 }

        mqttClient?.subscribe(topics, qos)

//        val balance = "${okKey}/${0}/req/balance"
        val registerOkKey = "${okKey}/${0}/req/register"

        val topicsArray = arrayOf(registerOkKey)

        publishTopicsSequentially(topicsArray, 0)
    }

    private fun subscribeContacts(contacts: HashMap<String, Int>) {
        val subscribeTopic = contacts.map { (key, value) ->
            "$key/$value/res/#"
        }.toTypedArray()

        val publishTopic = contacts.map { (key, value) ->
            "$key/$value/req/register"
        }.toTypedArray()

        val qos = IntArray(subscribeTopic.size) { 1 }

        mqttClient?.subscribe(subscribeTopic, qos)

        publishTopicsSequentially(publishTopic, 0)
    }

    private fun publishTopicsSequentially(topics: Array<String>, index: Int) {
        if (index < topics.size) {
            val topic = topics[index]
            val message = MqttMessage()

            mqttClient?.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // Recursively call the function with the next index
                    publishTopicsSequentially(topics, index + 1)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("MQTT_MESSAGES", "Failed to publish to $topic: ${exception?.message}")
                }
            })
        }
    }

    private fun handleMqttMessage(topic: String?, message: MqttMessage?) {
        topic ?: return

        parseTopic(topic)?.let { topicData ->
            val topicHandler = topicHandlers[topicData.third]

            val connectionState = topicHandler?.handle(
                topicData.first,
                topicData.second,
                topicData.third,
                message.toString()
            )

            when (connectionState) {
                is ConnectionState.ContactRegistered -> {
                    handleContactRegistered(topic, connectionState)
                }
                else -> {
                    _connectionStateStateFlow.value = connectionState
                }
            }
        }
    }

    private fun handleContactRegistered(topic: String, connectionState: ConnectionState.ContactRegistered) {
        val isNewContact = newContact?.childPubKey?.value?.let { childPubKey ->
            topic.contains(childPubKey)
        } ?: return

        if (isNewContact) {
            val scid = extractScid(connectionState.message)
            val updatedNewContact = newContact?.copy(
                scid = scid?.toShortChannelId(),
                contactRouteHint = retrieveLightningRouteHint(newContact?.senderLspPubKey?.value, scid)
            )

            _connectionStateStateFlow.value = updatedNewContact?.let {
                ConnectionState.NewContactRegistered(it)
            }
        } else {
            _connectionStateStateFlow.value = connectionState
        }
    }

    override fun sendKeyExchangeOnionMessage(
        keyExchangeMessage: String,
        hops: String,
        walletMnemonic: WalletMnemonic,
        okKey: String
    ) {
        coroutineScope.launch {

            val seed = try {
                mnemonicToSeed(walletMnemonic.value)
            } catch (e: Exception) {
                null
            }

            val now = getTimestampInMilliseconds()

            if (seed != null) {

                val onion = try {
                    createOnionMsg(
                        seed,
                        0.toUInt(),
                        now,
                        network,
                        hops,
                        keyExchangeMessage
                    )
                } catch (e: Exception) {
                    null
                }

                if (onion != null && mqttClient?.isConnected == true) {

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

    // Utility Methods
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
            Log.d("MQTT", "MQTT Client is not connected.")
        }
    }


    override fun setLspIp(ip: String) {
        mixer = ip
    }

    override fun retrieveLspIp(): String? {
        return mixer
    }

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

    private fun extractScid(input: String): String? {
        val pattern = """"scid":"(\d+)"""".toRegex()
        val matchResult = pattern.find(input)
        return matchResult?.groups?.get(1)?.value
    }

    private fun setupTopicHandlers(): Map<String, TopicHandler> {
        return mapOf(
            "register" to TopicHandler.RegisterHandler,
            "balance" to TopicHandler.BalanceHandler,
        )
    }

    // Returns key, index and action
    private fun parseTopic(topic: String?): Triple<String, Int, String>? {
        val parts = topic?.split("/") ?: return null

        if (parts.size < 4) return null
        val key = parts[0]
        val index = parts[1].toIntOrNull() ?: return null
        val action = parts[3]

        return Triple(key, index, action)
    }


}