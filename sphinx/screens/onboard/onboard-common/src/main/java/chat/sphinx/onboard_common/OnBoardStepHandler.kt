package chat.sphinx.onboard_common

import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.onboard_common.internal.json.Step1Json
import chat.sphinx.onboard_common.internal.json.toOnboardStep1
import chat.sphinx.onboard_common.internal.json.toStep1Json
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class OnBoardStepHandler @Inject constructor(
    private val authenticationStorage: AuthenticationStorage,
    private val moshi: Moshi,
    private val LOG: SphinxLogger,
    dispatchers: CoroutineDispatchers
): CoroutineDispatchers by dispatchers {

    companion object {
        private val lock = Mutex()

        private const val TAG = "OnBoardStepHandler"

        private const val KEY = "ON_BOARD_STEP"

        private const val STEP_1 = "STEP_1"
        private const val STEP_2 = "STEP_2"
        private const val STEP_3 = "STEP_3"
        private const val STEP_4 = "STEP_4"

        // Character lengths must stay the same for
        // onboard step retrieval to function properly
        private const val STEP_SIZE: Int = STEP_1.length
    }

    suspend fun persistOnBoardStep1Data(
        relayUrl: RelayUrl,
        authorizationToken: AuthorizationToken,
        inviterData: OnBoardInviterData?
    ): OnBoardStep.Step1? {
        lock.withLock {
            val inviterDataRealized: OnBoardInviterData = inviterData
                ?: OnBoardInviterData(
                    nickname = "Sphinx Support",
                    pubkey = LightningNodePubKey("023d70f2f76d283c6c4e58109ee3a2816eb9d8feb40b23d62469060a2b2867b77f"),
                    routeHint = null,
                    message = "Welcome to Sphinx",
                    action = null,
                    pin = null,
                )

            val step1 = OnBoardStep.Step1(
                relayUrl,
                authorizationToken,
                inviterDataRealized
            )

            val step1Json: String = try {
                withContext(default) {
                    moshi
                        .adapter(Step1Json::class.java)
                        .toJson(step1.toStep1Json())
                } ?: throw IOException("Failed to convert Step1Json data to String")
            } catch (e: Exception) {
                LOG.e(TAG, "Step1 Json Conversion Error", e)
                return null
            }

            authenticationStorage.putString(KEY, STEP_1 + step1Json)

            return step1
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun retrieveOnBoardStep(): OnBoardStep? =
        lock.withLock {
            authenticationStorage.getString(KEY, null)?.let { stepString ->
                try {
                    when (stepString.take(STEP_SIZE)) {
                        STEP_1 -> {
                            withContext(default) {
                                moshi
                                    .adapter(Step1Json::class.java)
                                    .fromJson(stepString.drop(STEP_SIZE))
                                    ?.toOnboardStep1()
                            }
                                ?: throw IOException("Failed to convert Step1Json string to OnBoardStep1")
                        }
                        STEP_2 -> {
                            null
                        }
                        STEP_3 -> {
                            null
                        }
                        STEP_4 -> {
                            null
                        }
                        else -> {
                            null
                        }
                    }
                } catch (e: Exception) {
                    LOG.e(TAG, "Failed to retrieve and convert OnBoardStep data", e)
                    null
                }
            }
        }
}
