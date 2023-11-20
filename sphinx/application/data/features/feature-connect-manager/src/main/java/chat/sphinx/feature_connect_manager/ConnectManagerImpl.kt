package chat.sphinx.feature_connect_manager

import android.util.Log
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_lightning.LightningRepository
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.example.concept_connect_manager.model.ConnectionState
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import uniffi.sphinxrs.mnemonicFromEntropy
import uniffi.sphinxrs.mnemonicToSeed
import uniffi.sphinxrs.pubkeyFromSeed
import uniffi.sphinxrs.rootSignMs
import uniffi.sphinxrs.xpubFromSeed
import java.security.SecureRandom

class ConnectManagerImpl(
    private val walletDataHandler: WalletDataHandler,
    private val contactRepository: ContactRepository,
    private val lightningRepository: LightningRepository,
    dispatchers: CoroutineDispatchers
): ConnectManager(),
    CoroutineDispatchers by dispatchers
{

    private var mixer: String? = null
    private var walletMnemonic: WalletMnemonic? = null
    private var mqttClient: MqttClient? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val network = "regtest"

    private val _connectionStateStateFlow = MutableStateFlow<ConnectionState?>(null)
    override val connectionStateStateFlow: StateFlow<ConnectionState?>
        get() = _connectionStateStateFlow

    // Core Functional Methods

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
                    0
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

    override fun connectToMQTT(
        serverURI: String,
        clientId: String,
        key: String,
        password: String,
        okKey: String,
        index: Int
    ) {

        mqttClient = try {
            MqttClient(serverURI, clientId, null)
        } catch (e: MqttException) {
            e.printStackTrace()
            return
        }

        val options = MqttConnectOptions().apply {
            this.userName = key
            this.password = password.toCharArray()
            this.keepAliveInterval = 60
        }

        try {
            mqttClient?.connect(options)

            if (mqttClient?.isConnected == true) {
                val topics = arrayOf(
                    "${okKey}/${index}/res/#"
                )
                val qos = IntArray(topics.size) { 1 }

                mqttClient?.subscribe(topics, qos)

                val balance = "${okKey}/${index}/req/balance"
                val registerOkKey = "${okKey}/${index}/req/register"

                mqttClient?.publish(balance, MqttMessage())
                mqttClient?.publish(registerOkKey, MqttMessage())
            }

            mqttClient?.setCallback(object : MqttCallback {

                override fun connectionLost(cause: Throwable?) {
                    // Implement reconnection logic here
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Handle incoming messages here
                    Log.d("MQTT_MESSAGES", "$topic")
                    Log.d("MQTT_MESSAGES", "$message")
                    Log.d("MQTT_MESSAGES", "${message?.payload}")

                    _connectionStateStateFlow.value = ConnectionState.MqttMessage(message.toString())
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Handle message delivery confirmation here
                }
            })

        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


    // Utility Methods

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

}