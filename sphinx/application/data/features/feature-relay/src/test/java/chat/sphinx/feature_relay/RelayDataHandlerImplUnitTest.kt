package chat.sphinx.feature_relay

import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.wrapper_relay.RelayUrl
import io.matthewnelson.k_openssl.KOpenSSL
import io.matthewnelson.test_feature_authentication_core.AuthenticationCoreDefaultsTestHelper
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

class RelayDataHandlerImplUnitTest: AuthenticationCoreDefaultsTestHelper() {

    companion object {
        private const val RAW_URL = "https://some-endpoint.chat:3001"
        private const val RAW_JWT = "gsaAiFtGG/RfsaO"
    }

    private val relayHandler: RelayDataHandler by lazy {
        RelayDataHandlerImpl(
            testStorage,
            testCoreManager,
            dispatchers,
            testHandler
        )
    }

    @Test
    fun `login is required for anything to work`() =
        testDispatcher.runBlockingTest {
            Assert.assertFalse(relayHandler.persistRelayUrl(RelayUrl(RAW_URL)))
            Assert.assertNull(relayHandler.retrieveRelayUrl())
            Assert.assertFalse(relayHandler.persistJavaWebToken(JavaWebToken(RAW_JWT)))
            Assert.assertNull(relayHandler.retrieveJavaWebToken())
        }

    @Test
    fun `persisted data is encrypted`() =
        testDispatcher.runBlockingTest {
            login()

            Assert.assertTrue(relayHandler.persistRelayUrl(RelayUrl(RAW_URL)))
            testStorage.getString(RelayDataHandlerImpl.RELAY_URL_KEY, null)?.let { encryptedUrl ->
                Assert.assertTrue(KOpenSSL.isSalted(encryptedUrl))
            } ?: Assert.fail("Failed to persist relay url to storage")

            Assert.assertTrue(relayHandler.persistJavaWebToken(JavaWebToken(RAW_JWT)))
            testStorage.getString(RelayDataHandlerImpl.RELAY_JWT_KEY, null)?.let { encryptedJwt ->
                Assert.assertTrue(KOpenSSL.isSalted(encryptedJwt))
            } ?: Assert.fail("Failed to persist relay jwt to storage")
        }

    @Test
    fun `clearing JavaWebToken updates storage properly`() =
        testDispatcher.runBlockingTest {
            login()

            relayHandler.persistJavaWebToken(JavaWebToken(RAW_JWT))
            testStorage.getString(RelayDataHandlerImpl.RELAY_JWT_KEY, null)?.let { encryptedJwt ->
                Assert.assertTrue(KOpenSSL.isSalted(encryptedJwt))
            } ?: Assert.fail("Failed to persist relay jwt to storage")

            relayHandler.persistJavaWebToken(null)
            val notInStorage = "NOT_IN_STORAGE"
            testStorage.getString(RelayDataHandlerImpl.RELAY_JWT_KEY, notInStorage).let { jwt ->
                // default value is returned if persisted value is null
                if (jwt != notInStorage) {
                    Assert.fail("Java Web Token was not cleared from storage")
                }
            }
        }
}
