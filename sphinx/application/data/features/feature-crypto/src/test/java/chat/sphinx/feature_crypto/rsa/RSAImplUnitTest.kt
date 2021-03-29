package chat.sphinx.feature_crypto.rsa

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_crypto.rsa.*
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.exception
import io.matthewnelson.test_concept_coroutines.CoroutineTestHelper
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RSAImplUnitTest: CoroutineTestHelper() {

    @Before
    fun setup() {
        setupCoroutineTestHelper()
    }

    @After
    fun tearDown() {
        tearDownCoroutineTestHelper()
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
}