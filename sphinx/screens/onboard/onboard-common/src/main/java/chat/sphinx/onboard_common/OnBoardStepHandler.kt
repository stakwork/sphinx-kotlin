package chat.sphinx.onboard_common

import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.onboard_common.internal.json.*
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_common.model.OnBoardStep
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayHMacKey
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_rsa.RsaPublicKey
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
        transportKey: RsaPublicKey?,
        hMacKey: RelayHMacKey?,
        inviterData: OnBoardInviterData?
    ): OnBoardStep.Step1_WelcomeMessage? {
        lock.withLock {
            val inviterDataRealized: OnBoardInviterData = inviterData
                ?: OnBoardInviterData(
                    nickname = "Sphinx Support",
                    pubkey = null,
                    routeHint = null,
                    message = "Welcome to Sphinx",
                    action = null,
                    pin = null,
                )

            val step1 = OnBoardStep.Step1_WelcomeMessage(
                relayUrl,
                authorizationToken,
                transportKey,
                hMacKey,
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

    suspend fun persistOnBoardStep2Data(inviterData: OnBoardInviterData): OnBoardStep.Step2_Name? {
        lock.withLock {

            val step2 = OnBoardStep.Step2_Name(inviterData)
            val step2Json: String = try {
                withContext(default) {
                    moshi
                        .adapter(Step2Json::class.java)
                        .toJson(Step2Json(inviterData.toInviteDataJson()))
                } ?: throw IOException("Failed to convert Step2Json data to String")
            } catch (e: Exception) {
                LOG.e(TAG, "Step2 Json Conversion Error", e)
                return null
            }

            authenticationStorage.putString(KEY, STEP_2 + step2Json)

            return step2
        }
    }

    suspend fun persistOnBoardStep3Data(inviterData: OnBoardInviterData): OnBoardStep.Step3_Picture? {
        lock.withLock {

            val step3 = OnBoardStep.Step3_Picture(inviterData)
            val step3Json: String = try {
                withContext(default) {
                    moshi
                        .adapter(Step3Json::class.java)
                        .toJson(Step3Json(inviterData.toInviteDataJson()))
                } ?: throw IOException("Failed to convert Step3Json data to String")
            } catch (e: Exception) {
                LOG.e(TAG, "Step3 Json Conversion Error", e)
                return null
            }

            authenticationStorage.putString(KEY, STEP_3 + step3Json)

            return step3
        }
    }

    suspend fun persistOnBoardStep4Data(inviterData: OnBoardInviterData): OnBoardStep.Step4_Ready? {
        lock.withLock {

            val step4 = OnBoardStep.Step4_Ready(inviterData)
            val step4Json: String = try {
                withContext(default) {
                    moshi
                        .adapter(Step4Json::class.java)
                        .toJson(Step4Json(inviterData.toInviteDataJson()))
                } ?: throw IOException("Failed to convert Step4Json data to String")
            } catch (e: Exception) {
                LOG.e(TAG, "Step4 Json Conversion Error", e)
                return null
            }

            authenticationStorage.putString(KEY, STEP_4 + step4Json)

            return step4
        }
    }

    suspend fun finishOnBoardSteps() {
        lock.withLock {
            authenticationStorage.removeString(KEY)
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
                            withContext(default) {
                                moshi
                                    .adapter(Step2Json::class.java)
                                    .fromJson(stepString.drop(STEP_SIZE))
                                    ?.invite_data_json
                                    ?.toOnBoardInviteData()
                                    ?.let { inviterData ->
                                        OnBoardStep.Step2_Name(inviterData)
                                    }
                            }
                                ?: throw IOException("Failed to convert Step2Json string to OnBoardStep2")
                        }
                        STEP_3 -> {
                            withContext(default) {
                                moshi
                                    .adapter(Step3Json::class.java)
                                    .fromJson(stepString.drop(STEP_SIZE))
                                    ?.invite_data_json
                                    ?.toOnBoardInviteData()
                                    ?.let { inviterData ->
                                        OnBoardStep.Step3_Picture(inviterData)
                                    }
                            }
                                ?: throw IOException("Failed to convert Step3Json string to OnBoardStep3")
                        }
                        STEP_4 -> {
                            withContext(default) {
                                moshi
                                    .adapter(Step4Json::class.java)
                                    .fromJson(stepString.drop(STEP_SIZE))
                                    ?.invite_data_json
                                    ?.toOnBoardInviteData()
                                    ?.let { inviterData ->
                                        OnBoardStep.Step4_Ready(inviterData)
                                    }
                            }
                                ?: throw IOException("Failed to convert Step4Json string to OnBoardStep4")
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
