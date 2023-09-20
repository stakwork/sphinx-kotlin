package chat.sphinx.signer_manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_crypter.NetworkQueryCrypter
import chat.sphinx.concept_network_query_crypter.model.SendSeedDto
import chat.sphinx.concept_signer_manager.CheckAdminCallback
import chat.sphinx.concept_signer_manager.SignerCallback
import chat.sphinx.concept_signer_manager.SignerManager
import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.wrapper_common.SignerTopics
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import chat.sphinx.wrapper_message_media.token.MediaUrl
import chat.sphinx.wrapper_relay.toRelayUrl
import chat.sphinx.wrapper_relay.withProtocol
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPackDynamicSerializer
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.crypto_common.extensions.toHex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONException
import org.json.JSONObject
import uniffi.sphinxrs.Keys
import uniffi.sphinxrs.VlsResponse
import uniffi.sphinxrs.deriveSharedSecret
import uniffi.sphinxrs.encrypt
import uniffi.sphinxrs.makeAuthToken
import uniffi.sphinxrs.mnemonicFromEntropy
import uniffi.sphinxrs.mnemonicToSeed
import uniffi.sphinxrs.nodeKeys
import uniffi.sphinxrs.pubkeyFromSecretKey
import java.security.SecureRandom
import java.util.Date
import kotlin.coroutines.CoroutineContext

class SignerManagerImpl(
    context: Context,
    private val dispatchers: CoroutineDispatchers
): SignerManager(), CoroutineScope {

    private lateinit var moshi: Moshi
    private var walletDataHandler: WalletDataHandler? = null
    private var networkQueryCrypter: NetworkQueryCrypter? = null
    private var networkQueryContact: NetworkQueryContact? = null

    private var seedDto = SendSeedDto()
    private var walletMnemonic: WalletMnemonic? = null

    private var mqttClient: MqttClient? = null

    override fun setWalletDataHandler(walletDataHandlerInstance: Any) {
        (walletDataHandlerInstance as WalletDataHandler).let {
            walletDataHandler = it
        }
    }

    override fun setMoshi(moshiInstance: Any) {
        (moshiInstance as Moshi).let {
            moshi = it
        }
    }

    override fun setNetworkQueryCrypter(networkQueryCrypterInstance: Any) {
        (networkQueryCrypterInstance as NetworkQueryCrypter).let {
            networkQueryCrypter = it
        }
    }

    override fun setNetworkQueryContact(networkQueryContactInstance: Any) {
        (networkQueryContactInstance as NetworkQueryContact).let {
            networkQueryContact = it
        }
    }

    override fun setSeedFromGlyph(
        mqtt: String,
        network: String,
        relay: String
    ) {
        seedDto.lightningNodeUrl = mqtt
        seedDto.network = network
        seedDto.relayUrl = relay.withProtocol
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchers.mainImmediate

    companion object {
        const val VLS_ERROR = "Error: VLS Failed: invalid sequence"

        const val SIGNING_DEVICE_SHARED_PREFERENCES = "signer_settings"

        const val SIGNER_CLIENT_ID_KEY = "signer_client_id"
        const val SIGNER_LSS_NONCE_KEY = "signer_lss_nonce"
        const val SIGNER_MUTATIONS_KEY = "signer_mutations"
        const val SIGNER_SEQUENCE_KEY = "signer_sequence"
        const val SIGNING_DEVICE_SETUP_KEY = "signing-device-setup"
        const val PHONE_SIGNER_SETUP_KEY = "phone-signer-setup"
    }

    private val appContext: Context = context.applicationContext

    private val signerSharedPreferences: SharedPreferences =
        appContext.getSharedPreferences(SIGNING_DEVICE_SHARED_PREFERENCES, Context.MODE_PRIVATE)

    private var setupSignerHardwareJob: Job? = null
    private var setupPhoneSignerJob: Job? = null


    override fun isPhoneSignerSettingUp() : Boolean {
        return mqttClient != null
    }

    override fun setupSignerHardware(signerCallback: SignerCallback) {
        if (setupSignerHardwareJob?.isActive == true) return

        resetSeedDto()
        resetMQTT()

        setupSignerHardwareJob = launch {
            signerCallback.checkNetwork {
                signerCallback.signingDeviceNetwork { networkName ->
                    seedDto.ssid = networkName

                    signerCallback.signingDevicePassword(networkName) { networkPass ->
                        seedDto.pass = networkPass

                        signerCallback.signingDeviceLightningNodeUrl { lightningNodeUrl ->
                            seedDto.lightningNodeUrl = lightningNodeUrl

                            signerCallback.signingDeviceCheckBitcoinNetwork(
                                network = { seedDto.network = it },
                                linkSigningDevice = { callback ->
                                    if (callback) {
                                        linkSigningDevice(signerCallback)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun linkSigningDevice(signerCallback: SignerCallback) {
        launch {
            val secKey = ByteArray(32)
            SecureRandom().nextBytes(secKey)

            val sk1 = secKey.toHex()
            val pk1 = pubkeyFromSecretKey(sk1)

            var pk2: String? = null

            if (pk1 == null) {
                signerCallback.failedToSetupSigningDevice("error generating secret key")
                resetSeedDto()
                return@launch
            }

            seedDto.pubkey = pk1

            if (
                seedDto.lightningNodeUrl == null ||
                seedDto.lightningNodeUrl?.isEmpty() == true
            ) {
                resetSeedDto()
                signerCallback.failedToSetupSigningDevice("lightning node URL can't be empty")
                return@launch
            }

            networkQueryCrypter?.getCrypterPubKey()?.collect { loadResponse ->
                when (loadResponse) {
                    is LoadResponse.Loading -> {}
                    is Response.Error -> {
                        resetSeedDto()
                        signerCallback.failedToSetupSigningDevice("error getting public key from hardware")
                    }

                    is Response.Success -> {
                        pk2 = loadResponse.value.pubkey
                    }
                }
            }

            pk2?.let { nnPk2 ->
                val sec1 = deriveSharedSecret(nnPk2, sk1)
                val seedAndMnemonic = generateAndPersistMnemonic(null, null)

                seedAndMnemonic.second?.let { mnemonic ->
                    signerCallback.showMnemonicToUser(mnemonic.value) { callback ->
                         if (callback) {
                             seedAndMnemonic.first?.let { seed ->
                                 encryptAndSendSeed(seed, sec1, signerCallback)
                             }
                         }
                    }
                }
            }
        }
    }

    private fun encryptAndSendSeed(
        seed: String,
        sec1: String,
        signerCallback: SignerCallback
    ) {
        launch {
            val nonce = ByteArray(12)
            SecureRandom().nextBytes(nonce)

            encrypt(seed, sec1, nonce.toHex()).let { cipher ->
                if (cipher.isNotEmpty()) {
                    seedDto.seed = cipher

                    signerCallback.sendingSeedToHardware()

                    networkQueryCrypter?.sendEncryptedSeed(seedDto)?.collect { loadResponse ->
                        when (loadResponse) {
                            is LoadResponse.Loading -> {}
                            is Response.Error -> {
                                resetSeedDto()
                                signerCallback.failedToSetupSigningDevice("error sending seed to hardware")
                            }

                            is Response.Success -> {
                                setSigningDeviceSetupDone(signerCallback)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setSigningDeviceSetupDone(
        signerCallback: SignerCallback
    ) {
        signerSharedPreferences.edit().putBoolean(SIGNING_DEVICE_SETUP_KEY, true)
            .let { editor ->
                if (!editor.commit()) {
                    editor.apply()
                }
                signerCallback.signingDeviceSuccessfullySet()
            }
    }

    override fun reset() {
        resetMQTT()
        resetSeedDto()
    }

    private fun resetSeedDto() {
        seedDto.seed = null
        seedDto.network = null
        seedDto.pass = null
        seedDto.ssid = null
    }

    private fun resetMQTT() {
        if (mqttClient?.isConnected == true) {
            mqttClient?.disconnect()
        }
        mqttClient = null
    }

    override fun setupPhoneSigner(mnemonicWords: String?, signerCallback: SignerCallback) {

        if (setupPhoneSignerJob?.isActive == true) return

        setupPhoneSignerJob = launch {
            val (seed, mnemonic) = generateAndPersistMnemonic(mnemonicWords, signerCallback)

            val keys: Keys? = try {
                nodeKeys(net = seedDto.network ?:"regtest", seed = seed!!)
            } catch (e: Exception) {
                println(e.message)
                null
            }

            val password: String? = try {
                makeAuthToken(ts = (Date().time / 1000).toUInt(), secret = keys?.secret!!)
            } catch (e: Exception) {
                println(e.message)
                null
            }

            if (keys != null && password != null) {
                connectToMQTTWith(keys, password, signerCallback)
            }
        }
    }

    private fun connectToMQTTWith(keys: Keys, password: String, signerCallback: SignerCallback) {
        seedDto.lightningNodeUrl?.let { lightningNodeUrl ->
            val serverURI = processLightningNodeUrl(lightningNodeUrl)
            val clientId = retrieveOrGenerateClientId()

            mqttClient = try {
                MqttClient(serverURI, clientId, null)
            } catch (e: MqttException) {
                reset()
                signerCallback.phoneSignerSetupError()
                return
            }

            val options = MqttConnectOptions().apply {
                this.userName = keys.pubkey
                this.password = password.toCharArray()
                this.keepAliveInterval = 60
            }

            try {
                mqttClient?.connect(options)

                if (mqttClient?.isConnected == true) {

                    val topics = arrayOf(
                        "${clientId}/${SignerTopics.VLS}",
                        "${clientId}/${SignerTopics.INIT_1_MSG}",
                        "${clientId}/${SignerTopics.INIT_2_MSG}",
                        "${clientId}/${SignerTopics.LSS_MSG}"
                    )
                    val qos = IntArray(topics.size) { 1 }

                    mqttClient?.subscribe(topics, qos)

                    val topic = "${clientId}/${SignerTopics.HELLO}"
                    val message = MqttMessage()

                    mqttClient?.publish(topic, message)

                }

                mqttClient?.setCallback(object : MqttCallback {

                    override fun connectionLost(cause: Throwable?) {
                        restartMQTT()
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val payload = message?.payload ?: byteArrayOf()
                        val modifiedTopic = topic?.replace("${clientId}/", "") ?: ""

                        if (topic?.contains("vls") == true) {
                            println("MQTT message arrived $topic")
                        }

                        processMessage(modifiedTopic, payload)
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                signerCallback.phoneSignerSuccessfullySet()

            } catch (e: MqttException) {
                e.printStackTrace()
                reset()
                signerCallback.phoneSignerSetupError()
            }
        } ?: run {
            reset()
            signerCallback.phoneSignerSetupError()
        }
    }

    private fun processLightningNodeUrl(lightningNodeUrl: String): String {
        return if (lightningNodeUrl.endsWith(":8883")) {
            "ssl://$lightningNodeUrl"
        } else {
            "tcp://$lightningNodeUrl"
        }
    }

    private fun processMessage(topic: String, payload: ByteArray) {
        launch {
            val (args, state) = argsAndState()

            val ret: VlsResponse? =
                try {
                    uniffi.sphinxrs.run(
                        topic,
                        args,
                        state,
                        payload,
                        retrieveSequence(),
                    )
                } catch (e: Exception) {
                    if (e.localizedMessage?.contains(VLS_ERROR) == true) {
                        restartMQTT()
                    }
                    null
                }

            ret?.let {
                storeMutations(it.state)

                val clientId = retrieveOrGenerateClientId()
                mqttClient?.publish("${clientId}/${it.topic}", MqttMessage(it.bytes))

                if (topic.contains(SignerTopics.VLS)) {
                    storeAndIncrementSequence(ret.sequence)
                }
            }
        }
    }

     private suspend fun argsAndState(): Pair<String, ByteArray> {
        val args = makeArgs()
        val stringArgs = argsToJson(args) ?: ""
        val mutationsState: Map<String, ByteArray> = retrieveMutations()

        val state = MsgPack.encodeToByteArray(MsgPackDynamicSerializer, mutationsState)

        return Pair(stringArgs, state)
    }

    private fun restartMQTT() {
        val editor = signerSharedPreferences.edit()

        editor.putString(SIGNER_MUTATIONS_KEY, "")
        editor.putInt(SIGNER_SEQUENCE_KEY, 0)
        editor.apply()

        val clientId = retrieveOrGenerateClientId()
        val topic = "${clientId}/${SignerTopics.HELLO}"
        val message = MqttMessage()

        mqttClient?.publish(topic, message)
    }


     private fun storeMutations(inc: ByteArray) {
        launch {
            try {
                val decoded = MsgPack.decodeFromByteArray(MsgPackDynamicSerializer, inc)

                (decoded as? MutableMap<String, ByteArray>)?.let {
                    storeMutationsOnSharedPreferences(it)
                }

            } catch (e: Exception) { }
        }
    }

    private suspend fun makeArgs(): Map<String, Any?> {
        val seedBytes = getStoredMnemonicAndSeed().first?.hexToByArray()
        val lssNonce = retrieveOrGenerateLssNonce()

        val defaultPolicy = mapOf(
            "msat_per_interval" to 21000000000L,
            "interval" to "daily",
            "htlc_limit_msat" to 1000000000L
        )

        val args = mapOf(
            "seed" to seedBytes,
            "network" to (seedDto.network ?: "regtest"),
            "policy" to defaultPolicy,
            "allowlist" to emptyList<Any>(),
            "timestamp" to Date().time / 1000L,
            "lss_nonce" to lssNonce
        )

        return args
    }

    private fun retrieveOrGenerateClientId(): String {
        signerSharedPreferences.getString(SIGNER_CLIENT_ID_KEY, null)?.let {
            return it
        }

        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        val newClientId = (1..20).map { allowedChars.random() }.joinToString("")

        val editor = signerSharedPreferences.edit()
        editor.putString(SIGNER_CLIENT_ID_KEY, newClientId)
        editor.apply()

        return newClientId
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun retrieveOrGenerateLssNonce(): List<Int> {
        val storedLssNonceString = signerSharedPreferences.getString(SIGNER_LSS_NONCE_KEY, "")
        val storedLssNonce = storedLssNonceString?.split(",")?.mapNotNull { it.toIntOrNull() }

        val result = if (!storedLssNonce.isNullOrEmpty()) {
            storedLssNonce
        } else {
            val editor = signerSharedPreferences.edit()
            val randomBytes = generateRandomBytes(32)
            val randomBytesString = randomBytes.joinToString(",") { it.toString() }

            editor.putString(SIGNER_LSS_NONCE_KEY, randomBytesString)
            editor.apply()

            randomBytes.map { it.toInt() }
        }

        return result
    }

    private fun storeMutationsOnSharedPreferences(newMutations: MutableMap<String, ByteArray>) {
        val existingMutations = retrieveMutations()
        existingMutations.putAll(newMutations)

        val encodedString = encodeMapToBase64(existingMutations)
        val editor = signerSharedPreferences.edit()

        editor.putString(SIGNER_MUTATIONS_KEY, encodedString)
        editor.apply()
    }

    private fun retrieveMutations(): MutableMap<String, ByteArray> {
        val encodedString = signerSharedPreferences.getString(SIGNER_MUTATIONS_KEY, null)

        val result = encodedString?.let {
            decodeBase64ToMap(it)
        } ?: mutableMapOf()

        return result
    }

    private fun storeAndIncrementSequence(sequence: UShort) {
        val newSequence = sequence.toInt().plus(1)
        val editor = signerSharedPreferences.edit()

        editor.putInt(SIGNER_SEQUENCE_KEY, newSequence)
        editor.apply()
    }

    private fun retrieveSequence(): UShort? {
        val sequence = signerSharedPreferences.getInt(SIGNER_SEQUENCE_KEY, 0)

        if (sequence == 0) {
            return null
        }

        return sequence.toUShort()
    }

    private fun encodeMapToBase64(map: MutableMap<String, ByteArray>): String {
        val encodedMap = mutableMapOf<String, String>()

        for ((key, value) in map) {
            encodedMap[key] = Base64.encodeToString(value, Base64.DEFAULT)
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
                val decodedValue = Base64.decode(encodedValue, Base64.DEFAULT)
                decodedMap[key] = decodedValue
            }
        } catch (e: JSONException) { }

        return decodedMap
    }

    private fun argsToJson(map: Map<String, Any?>): String? {
        val adapter = moshi.adapter(Map::class.java)
        return adapter.toJson(map)
    }

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

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun generateAndPersistMnemonic(
        mnemonicWords: String?,
        signerCallback: SignerCallback?
    ): Pair<String?, WalletMnemonic?> {
        var seed: String? = null

        coroutineScope {
            launch {
                walletMnemonic = run {
                    try {
                        mnemonicWords?.toWalletMnemonic()?.let { nnWalletMnemonic ->
                            nnWalletMnemonic
                        } ?: run {
                            val randomBytes = generateRandomBytes(16)
                            val randomBytesString = randomBytes.joinToString("") { it.toString(16).padStart(2, '0') }
                            val words = mnemonicFromEntropy(randomBytesString)

                            words.toWalletMnemonic()?.let { nnWalletMnemonic ->
                                signerCallback?.showMnemonicToUser(words) {}
                                nnWalletMnemonic
                            }
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
            }.join()
        }
        return Pair(seed, walletMnemonic)
    }

    private suspend fun getStoredMnemonicAndSeed(): Pair<String?, WalletMnemonic?> {
        var seed: String? = null

        (walletDataHandler?.retrieveWalletMnemonic() ?: walletMnemonic)?.value?.let { words ->
            try {
                seed = mnemonicToSeed(words)
            } catch (e: Exception) {}
        }

        return Pair(seed, walletMnemonic)
    }

    override suspend fun getPublicKeyAndRelayUrl(): Pair<String, String>? {
        var keys: Keys? = null

        launch {
            getStoredMnemonicAndSeed()?.first?.let { seed ->
                seedDto.network?.let { nnNetwork ->
                    keys = try {
                        nodeKeys(net = nnNetwork, seed = seed)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }.join()

        seedDto?.relayUrl?.let { relayUrl ->
            keys?.pubkey?.let { publicKey ->
                return Pair(publicKey, relayUrl)
            }
        }

        return null
    }

    private var hasAdminRetries = 0
    override suspend fun checkHasAdmin(
        checkAdminCallback: CheckAdminCallback
    ) {
        seedDto.relayUrl?.toRelayUrl()?.let { relayUrl ->
            if (hasAdminRetries < 50) {
                hasAdminRetries += 1

                networkQueryContact?.hasAdmin(
                    relayUrl
                )?.collect { loadResponse ->
                    when (loadResponse) {
                        is LoadResponse.Loading -> {}
                        is Response.Error -> {
                            checkHasAdmin(checkAdminCallback)
                        }

                        is Response.Success -> {
                            checkAdminCallback.checkAdminSucceeded()
                        }
                    }
                }
            } else {
                hasAdminRetries = 0
                checkAdminCallback.checkAdminFailed()
            }
        } ?: run {
            hasAdminRetries = 0
            checkAdminCallback.checkAdminFailed()
        }
    }

    override fun persistMnemonic() {
        walletMnemonic?.let {
            launch {
                val success = walletDataHandler?.persistWalletMnemonic(it) ?: false

                if (success) {
                    walletMnemonic = null
                    setPhoneSignerSetupDone()
                }
            }
        }
    }

    private fun setPhoneSignerSetupDone() {
        signerSharedPreferences.edit().putBoolean(PHONE_SIGNER_SETUP_KEY, true)
            .let { editor ->
                if (!editor.commit()) {
                    editor.apply()
                }
            }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.hexToByArray(): ByteArray {
    val byteIterator = chunkedSequence(2)
        .map { it.toInt(16).toByte() }
        .iterator()

    return ByteArray(length / 2) { byteIterator.next() }
}
