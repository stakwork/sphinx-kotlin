package chat.sphinx.feature_connect_manager

import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    dispatchers: CoroutineDispatchers
): ConnectManager(),
    CoroutineDispatchers by dispatchers
{

    private var walletMnemonic: WalletMnemonic? = null
    private var mqttClient: MqttClient? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mixer = "tcp://54.164.163.153:1883"
    private val network = "regtest"

    // Core Functional Methods

    override fun createAccount() {
        coroutineScope.launch {

            val seed = generateAndPersistMnemonic(null)

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

            if (xPub != null && sig != null && okKey != null) {

                connectToMQTT(
                    mixer,
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
    override suspend fun generateAndPersistMnemonic(mnemonicWords: String?): Pair<String?, WalletMnemonic?> {
        var seed: String? = null

        walletMnemonic = run {
            try {
                mnemonicWords?.toWalletMnemonic()?.let { nnWalletMnemonic ->
                    nnWalletMnemonic
                } ?: run {
                    val randomBytes = generateRandomBytes(16)
                    val randomBytesString =
                        randomBytes.joinToString("") { it.toString(16).padStart(2, '0') }
                    val words = mnemonicFromEntropy(randomBytesString)

                    words.toWalletMnemonic()// show mnemonic to user
                }
            } catch (e: Exception) {
                null
            }
        }

        walletMnemonic?.value?.let { words ->
            try {
                seed = mnemonicToSeed(words)
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