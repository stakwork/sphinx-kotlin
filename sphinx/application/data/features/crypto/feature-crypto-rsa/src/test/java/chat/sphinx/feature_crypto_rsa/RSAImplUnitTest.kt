package chat.sphinx.feature_crypto_rsa

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_crypto_rsa.*
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.test_network_query.NetworkQueryTestHelper
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_rsa.*
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import io.matthewnelson.crypto_common.annotations.UnencryptedDataAccess
import io.matthewnelson.crypto_common.clazzes.EncryptedString
import io.matthewnelson.crypto_common.clazzes.UnencryptedByteArray
import io.matthewnelson.crypto_common.clazzes.UnencryptedString
import io.matthewnelson.crypto_common.clazzes.toUnencryptedString
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

@RawPasswordAccess
@OptIn(UnencryptedDataAccess::class)
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
                """.trimIndent()
    }

    private val rsa: RSA by lazy {
        RSAImpl(RSAAlgorithm.RSA)
    }

    @Throws(AssertionError::class)
    private suspend fun generateKeys(
        keySize: KeySize,
        pkcsType: PKCSType
    ): RSAKeyPair {
        rsa.generateKeyPair(keySize, dispatchers.default, pkcsType).let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    println(response.message)
                    response.exception?.printStackTrace()
                    throw AssertionError(
                        """
                            Key Generation Failure For:
                            - KeySize: ${keySize.value}
                            - PKCSType: ${pkcsType.javaClass.simpleName}
                        """.trimIndent(),
                        response.exception
                    )
                }
                is Response.Success -> {
                    return response.value
                }
            }
        }
    }

    @Throws(AssertionError::class)
    private suspend fun decrypt(
        privateKey: RsaPrivateKey,
        encryptedString: EncryptedString
    ): UnencryptedByteArray {
        rsa.decrypt(
            rsaPrivateKey = privateKey,
            text = encryptedString,
            dispatcher = dispatchers.default
        ).let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    println(response.message)
                    response.exception?.printStackTrace()
                    throw AssertionError(
                        """
                            Decryption Failure For:
                            - EncryptedString: ${encryptedString.value}
                        """.trimIndent(),
                        response.exception
                    )
                }
                is Response.Success -> {
                    return response.value
                }
            }
        }
    }

    @Throws(AssertionError::class)
    private suspend fun encrypt(
        publicKey: RsaPublicKey,
        unencryptedString: UnencryptedString,
        formatOutput: Boolean = true
    ): EncryptedString {
        rsa.encrypt(
            rsaPublicKey = publicKey,
            text = unencryptedString,
            formatOutput = formatOutput,
            dispatcher = dispatchers.default
        ).let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    println(response.message)
                    response.exception?.printStackTrace()
                    throw AssertionError(
                        """
                            Encryption Failure For:
                            - UnencryptedString: ${unencryptedString.value}
                        """.trimIndent(),
                        response.exception
                    )
                }
                is Response.Success -> {
                    return response.value
                }
            }
        }
    }

    @Throws(AssertionError::class)
    private suspend fun sign(
        privateKey: RsaPrivateKey,
        text: String
    ): RsaSignedString {
        rsa.sign(
            rsaPrivateKey = privateKey,
            text = text,
            dispatcher = dispatchers.default
        ).let { response ->
            @Exhaustive
            when (response) {
                is Response.Error -> {
                    println(response.message)
                    response.exception?.printStackTrace()
                    throw AssertionError(
                        """
                            Failed To Sign For:
                            - String: $text
                        """.trimIndent(),
                        response.exception
                    )
                }
                is Response.Success -> {
                    return response.value
                }
            }
        }
    }

    private suspend fun signVerify(
        publicKey: RsaPublicKey,
        rsaSignedString: RsaSignedString
    ): Boolean {
        rsa.verifySignature(
            rsaPublicKey = publicKey,
            signedString = rsaSignedString,
            dispatcher = dispatchers.default
        ).let { response ->
            when (response) {
                is Response.Error -> {
                    println(response.message)
                    response.exception?.printStackTrace()
                    throw AssertionError(
                        """
                            Failed To Verify Signature For:
                            - String: ${rsaSignedString.text}
                        """.trimIndent(),
                        response.exception
                    )
                }
                is Response.Success -> {
                    return response.value
                }
            }
        }
    }

    @Test
    fun `key generation success`() =
        testDispatcher.runBlockingTest {
            generateKeys(KeySize._1024, PKCSType.PKCS1)
            generateKeys(KeySize._1024, PKCSType.PKCS8)
        }

    @Test
    fun `message decryption from linux client success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {

                fun printError() {
                    println("\n\n***********************************************")
                    println("      SPHINX_CHAT_LINUX_CLIENT_ENCRYPTED")
                    println("                  and/or")
                    println("      SPHINX_CHAT_LINUX_CLIENT_DECRYPTED\n")
                    println("    System environment variables are not set\n")
                    println("             Tests were run!!!")
                    println("***********************************************\n\n")
                }

                System.getenv("SPHINX_CHAT_LINUX_CLIENT_ENCRYPTED")?.let { encryptedMessage ->
                    System.getenv("SPHINX_CHAT_LINUX_CLIENT_DECRYPTED")?.let decrypted@ { expected ->
                        if (encryptedMessage.isEmpty() || expected.isEmpty()) {
                            printError()
                            return@decrypted
                        }
                        
                        decrypt(
                            RsaPrivateKey(testCoreManager.getEncryptionKey()!!.privateKey.value),
                            EncryptedString(encryptedMessage)
                        ).let { unencryptedByteArray ->
                            val decryptedString = unencryptedByteArray.toUnencryptedString().value
//                            println(decryptedString)
                            Assert.assertEquals(expected, decryptedString)
                        }
                    } ?: printError()
                } ?: printError()
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
                        is Response.Error -> {
                            println(response.message)
                            response.exception?.printStackTrace()
                            Assert.fail()
                        }
                        is Response.Success -> {
                            var breakPlease = 0

                            for (message in response.value.new_messages) {
                                if (!message.message_content.isNullOrEmpty()) {
                                    val decrypted = decrypt(
                                        privateKey,
                                        EncryptedString(message.message_content!!)
                                    )
//                                    println(decrypted.toUnencryptedString().value)
                                    breakPlease++
                                }

                                if (breakPlease > 4) {
                                    break
                                }
                            }

                            if (breakPlease <= 4) {
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
            // Generate new Public/Private key pair
            val keys = generateKeys(
                keySize = KeySize._4096,
                pkcsType = PKCSType.PKCS1
            )

            // Encrypt a message using the public key
            val encrypted = encrypt(
                publicKey = keys.publicKey,
                unencryptedString = UnencryptedString(TEST_MESSAGE_LARGE),
                formatOutput = true
            )


            // Decrypt the encrypted message using the private key
            val decrypted = decrypt(
                privateKey = keys.privateKey,
                encryptedString = encrypted
            )

            Assert.assertEquals(TEST_MESSAGE_LARGE, decrypted.toUnencryptedString().value)
        }

    @Test
    fun `signature verification success`() =
        testDispatcher.runBlockingTest {
            val keys = generateKeys(
                keySize = KeySize._1024,
                pkcsType = PKCSType.PKCS1
            )

            val signature = sign(
                privateKey = keys.privateKey,
                text = TEST_MESSAGE_SMALL
            )

            val verifySuccess = signVerify(
                publicKey = keys.publicKey,
                rsaSignedString = signature
            )
            Assert.assertTrue(verifySuccess)

            val verifyFailure = signVerify(
                publicKey = keys.publicKey,
                rsaSignedString = RsaSignedString(
                    signature.text.dropLast(1),
                    signature.signature
                )
            )
            Assert.assertFalse(verifyFailure)
        }
}
