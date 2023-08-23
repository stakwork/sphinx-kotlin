package chat.sphinx.concept_signer_manager

import chat.sphinx.wrapper_lightning.WalletMnemonic
import org.eclipse.paho.client.mqttv3.MqttClient
import uniffi.sphinxrs.Keys

abstract class SignerManager {

    abstract fun start()

    abstract fun connectToMQTTWith(keys: Keys, password: String)

    abstract fun processMessage(topic: String, payload: ByteArray, mqttClient: MqttClient)

    abstract suspend fun argsAndState(): Pair<String, ByteArray>

    abstract fun restart(mqttClient: MqttClient)

    abstract fun storeMutations(inc: ByteArray)

    abstract suspend fun makeArgs(): Map<String, Any>?

    abstract fun retrieveOrGenerateClientId(): String

    abstract fun retrieveOrGenerateLssNonce(): List<Int>

    abstract fun storeMutationsOnSharedPreferences(newMutations: MutableMap<String, ByteArray>)

    abstract fun retrieveMutations(): MutableMap<String, ByteArray>

    abstract fun storeAndIncrementSequence(sequence: UShort)

    abstract fun retrieveSequence(): UShort

    abstract fun encodeMapToBase64(map: MutableMap<String, ByteArray>): String

    abstract fun decodeBase64ToMap(encodedString: String): MutableMap<String, ByteArray>

    abstract fun argsToJson(map: Map<String, Any>?): String?

    abstract fun generateRandomBytes(): UByteArray

    abstract suspend fun generateAndPersistMnemonic() : Pair<String?, WalletMnemonic?>

}