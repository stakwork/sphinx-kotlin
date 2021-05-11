package chat.sphinx.feature_relay

import chat.sphinx.concept_network_tor.*
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.wrapper_relay.RelayUrl
import io.matthewnelson.k_openssl.KOpenSSL
import io.matthewnelson.k_openssl.isSalted
import io.matthewnelson.test_feature_authentication_core.AuthenticationCoreDefaultsTestHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

class RelayDataHandlerImplUnitTest: AuthenticationCoreDefaultsTestHelper() {

    companion object {
        private const val RAW_URL = "https://some-endpoint.chat:3001"
        private const val RAW_JWT = "gsaAiFtGG/RfsaO"
    }

    private class TestTorManager: TorManager {
        override val socksProxyAddressStateFlow: StateFlow<SocksProxyAddress?>
            get() = MutableStateFlow(null)

        override suspend fun getSocksPortSetting(): String {
            return TorManager.DEFAULT_SOCKS_PORT.toString()
        }

        override val torStateFlow: StateFlow<TorState>
            get() = MutableStateFlow(TorState.Off)
        override val torNetworkStateFlow: StateFlow<TorNetworkState>
            get() = MutableStateFlow(TorNetworkState.Disabled)
        override val torServiceStateFlow: StateFlow<TorServiceState>
            get() = MutableStateFlow(TorServiceState.OnDestroy(-1))

        override fun startTor() {}
        override fun stopTor() {}
        override fun restartTor() {}
        override fun newIdentity() {}

        private var torIsRequired: Boolean? = null
        override suspend fun setTorRequired(required: Boolean) {
            torIsRequired = required
        }

        override suspend fun isTorRequired(): Boolean? {
            return torIsRequired
        }

        override fun addTorManagerListener(listener: TorManagerListener): Boolean {
            return false
        }

        override fun removeTorManagerListener(listener: TorManagerListener): Boolean {
            return false
        }
    }

    private val relayHandler: RelayDataHandler by lazy {
        RelayDataHandlerImpl(
            testStorage,
            testCoreManager,
            dispatchers,
            testHandler,
            TestTorManager()
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
