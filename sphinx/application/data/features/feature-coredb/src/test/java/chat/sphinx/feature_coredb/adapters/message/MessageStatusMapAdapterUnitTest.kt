package chat.sphinx.feature_coredb.adapters.message

import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_message.MessageStatus
import org.junit.Assert
import org.junit.Test

class MessageStatusMapAdapterUnitTest {

    private val adapter = MessageStatusMapAdapter()
    private val testData = listOf(
        Pair(ContactId(1), MessageStatus.NoStatus),
        Pair(ContactId(2), MessageStatus.Pending),
        Pair(ContactId(3), MessageStatus.Confirmed),
        Pair(ContactId(4), MessageStatus.Cancelled),
        Pair(ContactId(5), MessageStatus.Received),
        Pair(ContactId(6), MessageStatus.Failed),
        Pair(ContactId(7), MessageStatus.Deleted),
    )

    @Test
    fun `multi-entry list encodes and decodes properly`() {
        val encoded: String = adapter.encode(testData)
        val decoded = adapter.decode(encoded)

        Assert.assertEquals(testData[0].second, decoded[0].second)
        Assert.assertEquals(testData[1].second, decoded[1].second)
        Assert.assertEquals(testData[2].second, decoded[2].second)
        Assert.assertEquals(testData[3].second, decoded[3].second)
        Assert.assertEquals(testData[4].second, decoded[4].second)
        Assert.assertEquals(testData[5].second, decoded[5].second)
        Assert.assertEquals(testData[6].second, decoded[6].second)
    }

    @Test
    fun `single entry encodes and decodes properly`() {
        val encoded = adapter.encode(listOf(testData[0]))
        val decoded = adapter.decode(encoded)

        Assert.assertEquals(testData[0].second, decoded[0].second)
    }

    @Test
    fun `empty list encodes and decodes properly`() {
        val encoded = adapter.encode(emptyList())
        val decoded = adapter.decode(encoded)

        Assert.assertTrue(decoded.isEmpty())
    }
}