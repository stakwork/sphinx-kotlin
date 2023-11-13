package chat.sphinx.feature_connect_manager

import chat.sphinx.concept_wallet.WalletDataHandler
import chat.sphinx.example.concept_connect_manager.ConnectManager
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import uniffi.sphinxrs.mnemonicFromEntropy
import uniffi.sphinxrs.mnemonicToSeed
import uniffi.sphinxrs.pubkeyFromSeed
import uniffi.sphinxrs.xpubFromSeed
import java.security.SecureRandom

class ConnectManagerImpl(
    private val walletDataHandler: WalletDataHandler
) : ConnectManager() {

    private var walletMnemonic: WalletMnemonic? = null

    // Core Functional Methods

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

    fun getTimestampInMilliseconds(): String =
        System.currentTimeMillis().toString()

}