package chat.sphinx.authentication

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import chat.sphinx.concept_coredb.SphinxDatabase
import chat.sphinx.database.SphinxCoreDBImplAndroid
import chat.sphinx.feature_coredb.SphinxCoreDBImpl
import chat.sphinx.feature_relay.RelayDataHandlerImpl
import chat.sphinx.key_restore.KeyRestoreResponse
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.k_openssl_common.clazzes.Password
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

    private val sphinxKeyHandler: SphinxEncryptionKeyHandler by lazy {
        SphinxEncryptionKeyHandler()
    }

    private inner class TestSphinxCoreDBImpl: SphinxCoreDBImpl() {

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

    private val testSphinxCoreDBImpl: TestSphinxCoreDBImpl by lazy {
        TestSphinxCoreDBImpl()
    }

    private val sphinxManager: SphinxAuthenticationCoreManager by lazy {
        SphinxAuthenticationCoreManager(
            app,
            dispatchers,
            sphinxKeyHandler,
            sphinxStorage,
            testSphinxCoreDBImpl,
        )
    }

    private val relayDataHandler: RelayDataHandlerImpl by lazy {
        RelayDataHandlerImpl(
            sphinxStorage,
            sphinxManager,
            dispatchers,
            sphinxKeyHandler,
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
        const val RAW_RELAY_URL = "https://chat.sphinx.something-relay:3001"
        val RAW_RELAY_JWT = "ginoi3n4k5podb4"
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