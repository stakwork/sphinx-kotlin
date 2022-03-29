package chat.sphinx.feature_relay

import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_crypto_rsa.RSAAlgorithm
import chat.sphinx.feature_crypto_rsa.RSAImpl
import chat.sphinx.test_tor_manager.TestTorManager
import chat.sphinx.wrapper_relay.RelayUrl
import io.matthewnelson.k_openssl.isSalted
import io.matthewnelson.test_feature_authentication_core.AuthenticationCoreDefaultsTestHelper
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

class RelayDataHandlerImplUnitTest: AuthenticationCoreDefaultsTestHelper() {

    companion object {
        private const val RAW_URL = "https://some-endpoint.chat:3001"
        private const val RAW_JWT = "gsaAiFtGG/RfsaO"
    }

    private val testTorManager: TorManager by lazy {
        TestTorManager()
    }

    private val testRSA: RSA by lazy {
        RSAImpl(RSAAlgorithm.RSA)
    }

    private val relayHandler: RelayDataHandler by lazy {
        RelayDataHandlerImpl(
            testStorage,
            testCoreManager,
            dispatchers,
            testHandler,
            testTorManager,
            testRSA
        )
    }

    @Test
    fun `login is required for anything to work`() =
        testDispatcher.runBlockingTest {
            Assert.assertFalse(relayHandler.persistRelayUrl(RelayUrl(RAW_URL)))
            Assert.assertNull(relayHandler.retrieveRelayUrl())
            Assert.assertFalse(relayHandler.persistAuthorizationToken(AuthorizationToken(RAW_JWT)))
            Assert.assertNull(relayHandler.retrieveAuthorizationToken())
        }

    @Test
    fun `persisted data is encrypted`() =
        testDispatcher.runBlockingTest {
            login()

            Assert.assertTrue(relayHandler.persistRelayUrl(RelayUrl(RAW_URL)))
            testStorage.getString(RelayDataHandlerImpl.RELAY_URL_KEY, null)?.let { encryptedUrl ->
                Assert.assertTrue(encryptedUrl.isSalted)
            } ?: Assert.fail("Failed to persist relay url to storage")

            Assert.assertTrue(relayHandler.persistAuthorizationToken(AuthorizationToken(RAW_JWT)))
            testStorage.getString(RelayDataHandlerImpl.RELAY_AUTHORIZATION_KEY, null)?.let { encryptedJwt ->
                Assert.assertTrue(encryptedJwt.isSalted)
            } ?: Assert.fail("Failed to persist relay jwt to storage")
        }

    @Test
    fun `clearing JavaWebToken updates storage properly`() =
        testDispatcher.runBlockingTest {
            login()

            relayHandler.persistAuthorizationToken(AuthorizationToken(RAW_JWT))
            testStorage.getString(RelayDataHandlerImpl.RELAY_AUTHORIZATION_KEY, null)?.let { encryptedJwt ->
                Assert.assertTrue(encryptedJwt.isSalted)
            } ?: Assert.fail("Failed to persist relay jwt to storage")

            relayHandler.persistAuthorizationToken(null)
            val notInStorage = "NOT_IN_STORAGE"
            testStorage.getString(RelayDataHandlerImpl.RELAY_AUTHORIZATION_KEY, notInStorage).let { jwt ->
                // default value is returned if persisted value is null
                if (jwt != notInStorage) {
                    Assert.fail("Java Web Token was not cleared from storage")
                }
            }
        }
}
