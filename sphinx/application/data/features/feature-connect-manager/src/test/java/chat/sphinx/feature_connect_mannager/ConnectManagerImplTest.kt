package chat.sphinx.feature_connect_mannager

import chat.sphinx.example.concept_connect_manager.model.ConnectionState
import chat.sphinx.wrapper_lightning.WalletMnemonic
import chat.sphinx.wrapper_lightning.toWalletMnemonic
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import org.junit.Test
import uniffi.sphinxrs.mnemonicFromEntropy
import uniffi.sphinxrs.mnemonicToSeed
import java.security.SecureRandom

class ConnectManagerImplTest {

    @Test
    fun testGenerateMnemonic() {
        val (seed, walletMnemonic) = generateMnemonic()

        assertNotNull("Mnemonic should not be null", walletMnemonic)
        assertNotNull("Seed should not be null", seed)

        assertTrue("Mnemonic should have a specific format or length", walletMnemonic?.value?.split(" ")?.size == 12)

        // Optionally, you can add more checks to validate the seed
    }



    @OptIn(ExperimentalUnsignedTypes::class)
    private fun generateMnemonic(): Pair<String?, WalletMnemonic?> {
        var seed: String? = null
        var walletMnemonic: WalletMnemonic? = null
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
            } catch (e: Exception) {}
        }

        return Pair(seed, walletMnemonic)
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
}