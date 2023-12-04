package chat.sphinx.feature_network_query_invite

import chat.sphinx.test_network_query.NetworkQueryTestHelper
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NetworkQueryInviteImplUnitTest: NetworkQueryTestHelper() {

    @Test
    fun `test stub`() =
        runTest {
            getCredentials()?.let {
                nqInvite
            }
        }
}