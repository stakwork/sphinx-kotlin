package chat.sphinx.test_network_query

import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_network_query_verify_external.NetworkQueryAuthorizeExternal
import chat.sphinx.concept_network_query_people.NetworkQueryPeople
import chat.sphinx.concept_network_query_redeem_badge_token.NetworkQueryRedeemBadgeToken
import chat.sphinx.concept_network_query_version.NetworkQueryVersion
import chat.sphinx.concept_network_relay_call.NetworkRelayCall
import chat.sphinx.concept_network_tor.*
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_crypto_rsa.RSAAlgorithm
import chat.sphinx.feature_crypto_rsa.RSAImpl
import chat.sphinx.feature_network_client.NetworkClientImpl
import chat.sphinx.feature_network_query_chat.NetworkQueryChatImpl
import chat.sphinx.feature_network_query_contact.NetworkQueryContactImpl
import chat.sphinx.feature_network_query_invite.NetworkQueryInviteImpl
import chat.sphinx.feature_network_query_lightning.NetworkQueryLightningImpl
import chat.sphinx.feature_network_query_message.NetworkQueryMessageImpl
import chat.sphinx.feature_network_query_redeem_badge_token.NetworkQueryRedeemBadgeTokenImpl
import chat.sphinx.feature_network_query_subscription.NetworkQuerySubscriptionImpl
import chat.sphinx.feature_network_query_verify_external.NetworkQueryAuthorizeExternalImpl
import chat.sphinx.feature_network_query_people.NetworkQueryPeopleImpl
import chat.sphinx.feature_network_query_version.NetworkQueryVersionImpl
import chat.sphinx.feature_network_relay_call.NetworkRelayCallImpl
import chat.sphinx.feature_relay.RelayDataHandlerImpl
import chat.sphinx.logger.LogType
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.test_tor_manager.TestTorManager
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import com.squareup.moshi.Moshi
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.crypto_common.clazzes.Password
import io.matthewnelson.test_feature_authentication_core.AuthenticationCoreDefaultsTestHelper
import io.matthewnelson.test_feature_authentication_core.TestEncryptionKeyHandler
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.Cache
import okio.base64.decodeBase64ToArray
import org.cryptonode.jncryptor.AES256JNCryptor
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/**
 * This class uses a test account setup on SphinxRelay to help ensure API compatibility.
 *
 * It is important that all tests related to use of this class **not** fail
 * if environment variables are not set.
 *
 * Wrapping tests in [getCredentials]?.let { credentials -> // my test } ensures
 * that test will simply notify that environment variables should be set with
 * their own test account credentials.
 * */
@Suppress("BlockingMethodInNonBlockingContext")
abstract class NetworkQueryTestHelper: AuthenticationCoreDefaultsTestHelper() {

    companion object {
        protected var privKey: String? = null
        protected var pubKey: String? = null
        protected var relayUrl: RelayUrl? = null
        protected var transportToken: TransportToken? = null
        protected var authorizationToken: AuthorizationToken? = null

        @BeforeClass
        @JvmStatic
        fun setupClassNetworkQueryTestHelper() {
            System.getenv("SPHINX_CHAT_KEY_EXPORT")?.let { export ->
                System.getenv("SPHINX_CHAT_EXPORT_PASS")?.toCharArray()?.let { pass ->
                    setProperties(export, pass)
                    return
                }
            }

            println("\n\n***********************************************")
            println("          SPHINX_CHAT_KEY_EXPORT")
            println("                   and")
            println("          SPHINX_CHAT_EXPORT_PASS\n")
            println("    System environment variables are not set\n")
            println("        Network Tests will not be run!!!")
            println("***********************************************\n\n")
        }

        fun setProperties(keyExport: String, password: CharArray) {
            keyExport
                .decodeBase64ToArray()
                ?.toString(charset("UTF-8"))
                ?.split("::")
                ?.let { decodedSplit ->
                    if (decodedSplit.elementAtOrNull(0) != "keys") {
                        return
                    }

                    decodedSplit.elementAt(1).decodeBase64ToArray()?.let { toDecrypt ->
                        val decryptedSplit = AES256JNCryptor()
                            .decryptData(toDecrypt, password)
                            .toString(charset("UTF-8"))
                            .split("::")

                        if (decryptedSplit.size != 4) {
                            return
                        }

                        privKey = decryptedSplit[0]
                        pubKey = decryptedSplit[1]
                        relayUrl = RelayUrl(decryptedSplit[2])
                        authorizationToken = AuthorizationToken(decryptedSplit[3])
                    }
                }
        }
    }

    private val testTorManager: TorManager by lazy {
        TestTorManager()
    }

    private val testRSA: RSA by lazy {
        RSAImpl(RSAAlgorithm.RSA)
    }

    protected data class Credentials(
        val privKey: String,
        val pubKey: String,
        val relayUrl: RelayUrl,
        val jwt: AuthorizationToken,
    )

    /**
     * Will return null if the SystemProperties for:
     *  - SPHINX_CHAT_KEY_EXPORT
     *  - SPHINX_CHAT_EXPORT_PASS
     *
     * are not set, allowing for a soft failure of the tests.
     * */
    protected fun getCredentials(): Credentials? =
        privKey?.let { nnPrivKey ->
            pubKey?.let { nnPubKey ->
                relayUrl?.let { nnRelayUrl ->
                    authorizationToken?.let { nnJwt ->
                        Credentials(
                            nnPrivKey,
                            nnPubKey,
                            nnRelayUrl,
                            nnJwt
                        )
                    }
                }
            }
        }

    protected open val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    private class TestSphinxLogger: SphinxLogger() {
        override fun log(tag: String, message: String, type: LogType, throwable: Throwable?) {}
    }

    protected open val testLogger: SphinxLogger by lazy {
        TestSphinxLogger()
    }

    /**
     * Override this and set to `true` to use Logging Interceptors during the test
     * */
    open val useLoggingInterceptors: Boolean = false

    @get:Rule
    val testDirectory = TemporaryFolder()

    open val okHttpCache: Cache by lazy {
        Cache(testDirectory.newFile("okhttp_test_cache"), 2000000L /*2MB*/)
    }

    protected open val networkClient: NetworkClient by lazy {
        NetworkClientImpl(
            // true will add interceptors to the OkHttpClient
            BuildConfigDebug(useLoggingInterceptors),
            okHttpCache,
            dispatchers,
            null,
            testTorManager,
            testLogger,
        )
    }

    protected open val relayDataHandler: RelayDataHandler by lazy {
        RelayDataHandlerImpl(
            testStorage,
            testCoreManager,
            dispatchers,
            testHandler,
            testTorManager,
            testRSA
        )
    }

    protected open val networkRelayCall: NetworkRelayCall by lazy {
        NetworkRelayCallImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler,
            testLogger
        )
    }

    protected open val nqChat: NetworkQueryChat by lazy {
        NetworkQueryChatImpl(networkRelayCall)
    }

    protected open val nqContact: NetworkQueryContact by lazy {
        NetworkQueryContactImpl(networkRelayCall)
    }

    protected open val nqInvite: NetworkQueryInvite by lazy {
        NetworkQueryInviteImpl(networkRelayCall)
    }

    protected open val nqMessage: NetworkQueryMessage by lazy {
        NetworkQueryMessageImpl(networkRelayCall)
    }

    protected open val nqSubscription: NetworkQuerySubscription by lazy {
        NetworkQuerySubscriptionImpl(networkRelayCall)
    }

    protected open val nqVersion: NetworkQueryVersion by lazy {
        NetworkQueryVersionImpl(networkRelayCall)
    }

    protected open val nqLightning: NetworkQueryLightning by lazy {
        NetworkQueryLightningImpl(networkRelayCall)
    }

    protected open val nqAuthorizeExternal: NetworkQueryAuthorizeExternal by lazy {
        NetworkQueryAuthorizeExternalImpl(networkRelayCall)
    }

    protected open val nqSaveProfile: NetworkQueryPeople by lazy {
        NetworkQueryPeopleImpl(networkRelayCall)
    }

    protected open val nqRedeemBadgeToken: NetworkQueryRedeemBadgeToken by lazy {
        NetworkQueryRedeemBadgeTokenImpl(networkRelayCall)
    }

    @Before
    fun setupNetworkQueryTestHelper() = testDispatcher.runBlockingTest {
        testDirectory.create()
        getCredentials()?.let { creds ->
            // Set our raw private/public keys in the test handler so when we login
            // for the first time the generated keys will be these
            testHandler.keysToRestore = TestEncryptionKeyHandler.RestoreKeyHolder(
                Password(creds.privKey.toCharArray()),
                Password(creds.pubKey.toCharArray())
            )

            // login for the first time to setup the authentication library with
            // a pin of 000000
            login()

            // persist our relay url and java web token to test storage
            relayDataHandler.persistAuthorizationToken(creds.jwt)
            relayDataHandler.persistRelayUrl(creds.relayUrl)
        }

        // if null, do nothing.
    }

    @After
    fun tearDownNetworkQueryTestHelper() {
        testDirectory.delete()
    }
}
