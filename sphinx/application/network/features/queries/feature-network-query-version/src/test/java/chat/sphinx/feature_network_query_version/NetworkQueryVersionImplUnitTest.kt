package chat.sphinx.feature_network_query_version

import app.cash.exhaustive.Exhaustive
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.test_network_query.NetworkQueryTestHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

class NetworkQueryVersionImplUnitTest: NetworkQueryTestHelper() {

    @Test
    fun `getAppVersions returns success`() =
        runTest {
            getCredentials()?.let {
                nqVersion.getAppVersions().collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            // will fail on error
                            loadResponse.exception?.printStackTrace()
                            fail(loadResponse.message)
                        }
                        is Response.Success -> {
                        }
                        is LoadResponse.Loading -> {
                        }
                    }

                }
            }
        }
}