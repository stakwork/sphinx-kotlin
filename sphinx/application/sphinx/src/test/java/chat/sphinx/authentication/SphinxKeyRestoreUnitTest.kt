package chat.sphinx.authentication

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import chat.sphinx.concept_coredb.SphinxDatabase
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.feature_coredb.CoreDBImpl
import chat.sphinx.feature_crypto_rsa.RSAAlgorithm
import chat.sphinx.feature_crypto_rsa.RSAImpl
import chat.sphinx.feature_network_tor.TorManagerAndroid
import chat.sphinx.feature_relay.RelayDataHandlerImpl
import chat.sphinx.feature_sphinx_service.ApplicationServiceTracker
import chat.sphinx.key_restore.KeyRestoreResponse
import chat.sphinx.test_tor_manager.TestTorManager
import chat.sphinx.util.SphinxLoggerImpl
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.test_concept_coroutines.CoroutineTestHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(minSdk = 28, maxSdk = 28, manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SphinxKeyRestoreUnitTest: CoroutineTestHelper() {

    private val app: Application by lazy {
        ApplicationProvider.getApplicationContext()
    }

    private inner class TestSphinxStorage: SphinxAuthenticationCoreStorage(app, dispatchers) {

        // Replace EncryptedSharedPreferences with regular for testing
        override val authenticationPrefs: SharedPreferences by lazy {
            app.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        }
    }

    private val sphinxStorage: SphinxAuthenticationCoreStorage by lazy {
        TestSphinxStorage()
    }

    private val rsa: RSA by lazy {
        RSAImpl(RSAAlgorithm.RSA_ECB_PKCS1Padding)
    }

    private val sphinxKeyHandler: SphinxEncryptionKeyHandler by lazy {
        SphinxEncryptionKeyHandler(rsa)
    }

    private inner class TestCoreDBImpl(moshi: Moshi): CoreDBImpl(moshi) {

        private var driver: AndroidSqliteDriver? = null

        override fun getSqlDriver(encryptionKey: EncryptionKey): SqlDriver {
            return driver ?: synchronized(this) {
                AndroidSqliteDriver(
                    SphinxDatabase.Schema,
                    app,
                    DB_NAME,
                ).also { driver = it }
            }
        }
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    private val testSphinxCoreDBImpl: TestCoreDBImpl by lazy {
        TestCoreDBImpl(moshi)
    }

    private val serviceTracker: ApplicationServiceTracker by lazy {
        object : ApplicationServiceTracker() {

        }
    }

    private val sphinxManager: SphinxAuthenticationCoreManager by lazy {
        SphinxAuthenticationCoreManager(
            app,
            serviceTracker,
            dispatchers,
            sphinxKeyHandler,
            sphinxStorage,
            testSphinxCoreDBImpl,
        )
    }

    private val testTorManager: TorManager by lazy {
        TestTorManager()
    }

    private val relayDataHandler: RelayDataHandlerImpl by lazy {
        RelayDataHandlerImpl(
            sphinxStorage,
            sphinxManager,
            dispatchers,
            sphinxKeyHandler,
            testTorManager,
        )
    }

    private val sphinxKeyRestore: SphinxKeyRestore by lazy {
        SphinxKeyRestore(
            sphinxManager,
            sphinxStorage,
            sphinxKeyHandler,
            relayDataHandler
        )
    }

    private companion object {
        val TEST_PRIVATE_KEY = Password("laksdf09j32ipijoiwoihgoiwh4ithip9gpsigagadfg".toCharArray())
        val TEST_PUBLIC_KEY = Password("asdfoinavanlgknlgnlkanslgigjo23weojpasjd".toCharArray())
        val TEST_PIN = "012345".toCharArray()
        val RAW_RELAY_URL = RelayUrl("https://chat.sphinx.something-relay:3001")
        val RAW_RELAY_JWT = AuthorizationToken("ginoi3n4k5podb4")
    }

    @Before
    fun setup() {
        setupCoroutineTestHelper()
    }

    @After
    fun tearDown() {
        tearDownCoroutineTestHelper()
    }

    // TODO: Build Out Moar Tests for error handle checking.
    @Test
    fun `restoring keys returns success`() =
        testDispatcher.runBlockingTest {
            var success: KeyRestoreResponse.Success? = null
            sphinxKeyRestore.restoreKeys(
                TEST_PRIVATE_KEY,
                TEST_PUBLIC_KEY,
                TEST_PIN,
                RAW_RELAY_URL,
                RAW_RELAY_JWT
            ).collect { responseFlow ->
                if (responseFlow is KeyRestoreResponse.Success) {
                    success = responseFlow
                }
                println(responseFlow.javaClass.simpleName)
            }
            Assert.assertNotNull(success)
        }
}