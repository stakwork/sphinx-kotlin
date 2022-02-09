package chat.sphinx.feature_network_query_redeem_badge_token

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_query_redeem_badge_token.model.RedeemBadgeTokenDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.exception
import chat.sphinx.kotlin_response.message
import chat.sphinx.test_network_query.NetworkQueryTestHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class NetworkQueryRedeemBadgeTokenImplUnitTest: NetworkQueryTestHelper() {

    @Test
    fun `verifyExternal returns success`() =
        testDispatcher.runBlockingTest {
            getCredentials()?.let {

                var data = RedeemBadgeTokenDto(0,"https://sphinx.chat","sampleName","","", listOf(),0,"")
                nqRedeemBadgeToken.redeemBadgeToken(data).collect { loadResponse ->

                    @Exhaustive
                    when (loadResponse) {
                        is Response.Error -> {
                            loadResponse.exception?.printStackTrace()
                            Assert.fail(loadResponse.message)
                        }
                        is Response.Success -> {}
                        is LoadResponse.Loading -> {}
                    }

                }

            }
        }
} 
