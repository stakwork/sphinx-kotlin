package chat.sphinx.feature_background_login

import chat.sphinx.concept_background_login.BackgroundLoginHandler
import io.matthewnelson.test_feature_authentication_core.AuthenticationCoreDefaultsTestHelper
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

class BackgroundLoginHandlerImplUnitTest: AuthenticationCoreDefaultsTestHelper() {

    private val backgroundLoginHandler: BackgroundLoginHandler = BackgroundLoginHandlerImpl(
        testCoreManager,
        testStorage
    )

    @Test
    fun `everything fails if not logged in for first time`() =
        testDispatcher.runBlockingTest {
            Assert.assertNull(backgroundLoginHandler.attemptBackgroundLogin())
            Assert.assertFalse(backgroundLoginHandler.updateLoginTime())
            Assert.assertFalse(backgroundLoginHandler.updateSetting(2))
            Assert.assertTrue(testStorage.storage.isEmpty())
            Assert.assertEquals(
                BackgroundLoginHandlerImpl.DEFAULT_TIMEOUT,
                backgroundLoginHandler.getTimeOutSetting()
            )
            Assert.assertTrue(testStorage.storage.isEmpty())
        }
}