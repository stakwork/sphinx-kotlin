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
        val TEST_MESSAGE_LARGE = "" +
                """
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                    Maecenas eleifend dignissim tellus at auctor. Suspendisse
                    eget diam sit amet arcu scelerisque vehicula quis sed enim.
                    Vivamus molestie ligula vel dapibus gravida. In hac habitasse
                    platea dictumst. Nulla feugiat condimentum viverra. Duis a
                    fringilla ipsum. Nulla fringilla sodales tellus in dictum.
                    Proin nisi felis, auctor ac erat nec, molestie lobortis nulla.
                    Praesent ut dignissim libero, in mattis odio. In viverra eu
                    ligula in pretium.

                    Phasellus in sem malesuada, malesuada arcu in, tincidunt odio.
                    Aenean massa erat, lobortis ut finibus in, consequat vel nulla.
                    Phasellus lobortis, ipsum et maximus porttitor, purus tellus
                    sollicitudin purus, id hendrerit enim ante ac lorem. Vivamus
                    rutrum ex at dui mollis, vitae consequat turpis efficitur.
                    Donec molestie volutpat ligula, ac sollicitudin erat mollis in.
                    Duis vitae congue elit. Duis luctus ex justo, et bibendum
                    ligula aliquet at. Nullam ipsum eros, fermentum a tempus id,
                    molestie vel nibh. Sed tincidunt massa sit amet mattis vehicula.
                    Duis ac mauris eu turpis mollis ultricies ut vitae velit. Sed
                    sed metus eu ante pellentesque pellentesque non ac urna. Nam
                    nisl ex, pretium sit amet erat et, condimentum molestie lorem.
                    Vestibulum at nisl vestibulum, tempor augue eu, tristique diam.
                    Vivamus condimentum ex a sem ultrices mollis.

                    Cras et malesuada lorem, nec eleifend est. Nulla at orci a mi
                    ullamcorper pretium. Duis a ante ac odio congue ultricies a a
                    nisl. Nulla facilisi. Nam feugiat dolor nulla, et venenatis arcu
                    consectetur sed. Vivamus luctus urna ut massa euismod, in
                    dignissim nulla pellentesque. Integer velit odio, tincidunt a
                    imperdiet non, egestas at ligula.
                """.trimIndent()
    }

    private val rsa: RSA by lazy {
        RSAImpl()
    }

    @Test
    fun `key generation success`() =
        testDispatcher.runBlockingTest {
            rsa.generateKeyPair(KeySize._1024, dispatchers.default, PKCSType.PKCS1).let { response ->
                @Exhaustive
                when (response) {
                    is KotlinResponse.Error -> {
                        response.exception?.printStackTrace()
                        Assert.fail()
                    }
                    is KotlinResponse.Success -> {}
                }
            }
            rsa.generateKeyPair(KeySize._1024, dispatchers.default, PKCSType.PKCS8).let { response ->
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
                                        rsaPrivateKey = privateKey,
                                        text = EncryptedString(message.message_content!!),
                                        dispatcher = dispatchers.default
                                    ).let { decrypted ->
                                        @Exhaustive
                                        when (decrypted) {
                                            is KotlinResponse.Error -> {
                                                println(decrypted.message)
                                                decrypted.exception?.printStackTrace()
                                                Assert.fail()
                                            }
                                            is KotlinResponse.Success -> {
//                                                println(decrypted.value.toUnencryptedString().value)
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
            rsa.generateKeyPair(
                keySize = KeySize._4096,
                dispatcher = dispatchers.default,
                pkcsType = PKCSType.PKCS8
            ).let { response ->
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
                text = UnencryptedString(TEST_MESSAGE_LARGE),
//                text = UnencryptedString(TEST_MESSAGE_SMALL),
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
                        println(response.value.value)
                    }
                }
            }

            val enc: EncryptedString = encrypted ?: throw AssertionError()

            // Decrypt the encrypted message using the private key
            rsa.decrypt(
                rsaPrivateKey = rsaPriv,
                text = enc,
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
                        Assert.assertEquals(TEST_MESSAGE_LARGE, response.value.toUnencryptedString().value)
//                        Assert.assertEquals(TEST_MESSAGE_SMALL, response.value.toUnencryptedString().value)
                    }
                }
            }
        }

    @Test
    fun `signature verification success`() =
        testDispatcher.runBlockingTest {
            var privateKey: RsaPrivateKey? = null
            var publicKey: RsaPublicKey? = null

            // Generate new Public/Private key pair
            rsa.generateKeyPair(
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
                        privateKey = response.value.privateKey
                        publicKey = response.value.publicKey
                    }
                }
            }

            val rsaPriv: RsaPrivateKey = privateKey ?: throw AssertionError()
            val rsaPub: RsaPublicKey = publicKey ?: throw AssertionError()

            var signature: RsaSignedString? = null

            // Sign string value
            rsa.sign(
                rsaPrivateKey = rsaPriv,
                text = TEST_MESSAGE_SMALL,
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
                        signature = response.value
//                        println(response.value)
                    }
                }
            }

            val sig: RsaSignedString = signature ?: throw AssertionError()

            // Verify Success
            rsa.verifySignature(
                rsaPublicKey = rsaPub,
                signedString = sig,
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
                        Assert.assertTrue(response.value)
                    }
                }
            }

            // Verify Failure
            rsa.verifySignature(
                rsaPublicKey = rsaPub,
                signedString = RsaSignedString(
                    TEST_MESSAGE_SMALL.dropLast(1),
                    sig.signature
                ),
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
                        Assert.assertFalse(response.value)
                    }
                }
            }
        }
}
