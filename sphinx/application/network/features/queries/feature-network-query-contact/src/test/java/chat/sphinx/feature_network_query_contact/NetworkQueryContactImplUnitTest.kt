package chat.sphinx.feature_network_query_contact

import app.cash.exhaustive.Exhaustive
import chat.sphinx.kotlin_response.KotlinResponse
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.test_network_query.NetworkQueryTestHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

class NetworkQueryContactImplUnitTest: NetworkQueryTestHelper() {

    @Test
    fun `getContacts returns success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {
                nqContact.getContacts().collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is KotlinResponse.Error -> {
                            loadResponse.exception?.printStackTrace()
                            Assert.fail(loadResponse.message)
                        }
                        is KotlinResponse.Success -> {}
                        is LoadResponse.Loading -> {}
                    }

                }
            }
        }
}