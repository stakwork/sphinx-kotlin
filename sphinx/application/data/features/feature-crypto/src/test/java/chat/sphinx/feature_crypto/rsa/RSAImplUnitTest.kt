package chat.sphinx.feature_crypto.rsa

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_crypto.rsa.*
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.test_network_query.NetworkQueryTestHelper
import chat.sphinx.wrapper_common.message.MessagePagination
import io.matthewnelson.k_openssl_common.annotations.RawPasswordAccess
import io.matthewnelson.k_openssl_common.annotations.UnencryptedDataAccess
import io.matthewnelson.k_openssl_common.clazzes.EncryptedString
import io.matthewnelson.k_openssl_common.clazzes.UnencryptedString
import io.matthewnelson.k_openssl_common.clazzes.toUnencryptedString
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

@OptIn(RawPasswordAccess::class, UnencryptedDataAccess::class)
class RSAImplUnitTest: NetworkQueryTestHelper() {

    companion object {
        const val TEST_MESSAGE_SMALL = "TEST MESSAGE_SMALL"
    }

    private val rsa: RSA by lazy {
        RSAImpl()
    }

    @Test
    fun `2048 size key generation success`() =
        testDispatcher.runBlockingTest {
            rsa.generateKeyPair(KeySize._2048, dispatchers.default, PKCSType.PKCS1).let { response ->
                @Exhaustive
                when (response) {
                    is KotlinResponse.Error -> {
                        response.exception?.printStackTrace()
                        Assert.fail()
                    }
                    is KotlinResponse.Success -> {}
                }
            }
            rsa.generateKeyPair(KeySize._2048, dispatchers.default, PKCSType.PKCS8).let { response ->
                @Exhaustive
                when (response) {
                    is KotlinResponse.Error -> {
                        response.exception?.printStackTrace()
                        Assert.fail()
                    }
                    is KotlinResponse.Success -> {}
                }
            }
        }

    @Test
    fun `4096 size key generation success`() =
        testDispatcher.runBlockingTest {
            rsa.generateKeyPair(KeySize._4096, dispatchers.default, PKCSType.PKCS1).let { response ->
                @Exhaustive
                when (response) {
                    is KotlinResponse.Error -> {
                        response.exception?.printStackTrace()
                        Assert.fail()
                    }
                    is KotlinResponse.Success -> {}
                }
            }
            rsa.generateKeyPair(KeySize._4096, dispatchers.default, PKCSType.PKCS8).let { response ->
                @Exhaustive
                when (response) {
                    is KotlinResponse.Error -> {
                        response.exception?.printStackTrace()
                        Assert.fail()
                    }
                    is KotlinResponse.Success -> {}
                }
            }
        }

    @Test
    fun `8192 size key generation success`() =
        testDispatcher.runBlockingTest {
            rsa.generateKeyPair(KeySize._8192, dispatchers.default, PKCSType.PKCS1).let { response ->
                @Exhaustive
                when (response) {
                    is KotlinResponse.Error -> {
                        response.exception?.printStackTrace()
                        Assert.fail()
                    }
                    is KotlinResponse.Success -> {}
                }
            }
//            rsa.generateKeyPair(KeySize._8192, dispatchers.default, PKCSType.PKCS8).let { response ->
//                @Exhaustive
//                when (response) {
//                    is KotlinResponse.Error -> {
//                        response.exception?.printStackTrace()
//                        Assert.fail()
//                    }
//                    is KotlinResponse.Success -> {}
//                }
//            }
        }

    @Test
    fun `message decryption success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let { creds ->
                val privateKey = RsaPrivateKey(creds.privKey.toCharArray())

                nqMessage.getMessages(MessagePagination.instantiate(100, 500, null)).collect { response ->
                    @Exhaustive
                    when (response) {
                        is KotlinResponse.Error -> {
                            println(response.message)
                            response.exception?.printStackTrace()
                            Assert.fail()
                        }
                        is KotlinResponse.Success -> {
                            var breakPlease = false

                            for (message in response.value.new_messages) {
                                if (!message.message_content.isNullOrEmpty()) {
                                    rsa.decrypt(
                                        privateKey,
                                        EncryptedString(message.message_content!!),
                                        dispatchers.default
                                    ).let { decrypted ->
                                        @Exhaustive
                                        when (decrypted) {
                                            is KotlinResponse.Error -> {
                                                println(decrypted.message)
                                                decrypted.exception?.printStackTrace()
                                                Assert.fail()
                                            }
                                            is KotlinResponse.Success -> {
                                                println(decrypted.value.toUnencryptedString().value)
                                                breakPlease = true
                                            }
                                        }
                                    }
                                }

                                if (breakPlease) {
                                    break
                                }
                            }

                            if (!breakPlease) {
                                println("\n\n***********************************************")
                                println("                 WARNING\n")
                                println("    Test Account's new_messages list was empty\n")
                                println("     and message decryption was not tested!!!")
                                println("***********************************************\n\n")
                            }
                        }
                        is LoadResponse.Loading -> {}
                    }
                }
            }
        }

    @Test
    fun `public private key encryption success`() =
        testDispatcher.runBlockingTest {
            var privateKey: RsaPrivateKey? = null
            var publicKey: RsaPublicKey? = null

            // Generate new Public/Private key pair
            rsa.generateKeyPair(dispatcher = dispatchers.default).let { response ->
                @Exhaustive
                when (response) {
                    is KotlinResponse.Error -> {
                        println(response.message)
                        response.exception?.printStackTrace()
                        Assert.fail()
                    }
                    is KotlinResponse.Success -> {
                        privateKey = response.value.privateKey
                        publicKey = response.value.publicKey
                    }
                }
            }

            val rsaPriv: RsaPrivateKey = privateKey ?: throw AssertionError()
            val rsaPub: RsaPublicKey = publicKey ?: throw AssertionError()

            var encrypted: EncryptedString? = null

            // Encrypt a message using the public key
            rsa.encrypt(
                rsaPublicKey = rsaPub,
                text = UnencryptedString(TEST_MESSAGE_SMALL),
                formatOutput = true,
                dispatcher = dispatchers.default
            ).let { response ->
                @Exhaustive
                when (response) {
                    is KotlinResponse.Error -> {
                        println(response.message)
                        response.exception?.printStackTrace()
                        Assert.fail()
                    }
                    is KotlinResponse.Success -> {
                        encrypted = response.value
                    }
                }
            }

            val enc: EncryptedString = encrypted ?: throw AssertionError()

            // Decrypt the encrypted message using the private key
            rsa.decrypt(rsaPriv, enc, dispatchers.default).let { response ->
                @Exhaustive
                when (response) {
                    is KotlinResponse.Error -> {
                        println(response.message)
                        response.exception?.printStackTrace()
                        Assert.fail()
                    }
                    is KotlinResponse.Success -> {
                        Assert.assertEquals(TEST_MESSAGE_SMALL, response.value.toUnencryptedString().value)
                    }
                }
            }
        }
}
