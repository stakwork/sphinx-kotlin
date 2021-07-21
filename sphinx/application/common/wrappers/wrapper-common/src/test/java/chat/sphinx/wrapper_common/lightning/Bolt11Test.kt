package chat.sphinx.wrapper_common.lightning

import org.junit.Assert
import org.junit.Test

internal class Bolt11Test {
    @Test
    fun `Decode payment hash and short description`() {
        val bolt11 = Bolt11.decode(
            LightningPaymentRequest(
                "lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq8rkx3yf5tcsyz3d73gafnh3cax9rn449d9p5uxz9ezhhypd0elx87sjle52x86fux2ypatgddc6k63n7erqz25le42c4u4ecky03ylcqca784w"
            )
        )
        Assert.assertEquals("lnbc", bolt11.prefix)
        Assert.assertNull(bolt11.amount)
        Assert.assertEquals(1496314658,bolt11.timestampSeconds)

        Assert.assertEquals(2, bolt11.tags.size)
        Assert.assertEquals(Bolt11.TaggedField.PaymentHash.tag, bolt11.tags.first().tag)
        Assert.assertEquals(Bolt11.TaggedField.Description.tag, bolt11.tags.last().tag)
        Assert.assertEquals(
            "38ec6891345e204145be8a3a99de38e98a39d6a569434e1845c8af7205afcfcc7f425fcd1463e93c32881ead0d6e356d467ec8c02553f9aab15e5738b11f127f",
            bolt11.signature
        )
        Assert.assertEquals(0.toByte(), bolt11.recoveryFlag)
        Assert.assertEquals("ca784w", bolt11.checksum)
    }

    @Test
    fun `Decode amount, payment hash, short description, expiry time`() {
        val bolt11 = Bolt11.decode(
            LightningPaymentRequest(
                "lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdq5xysxxatsyp3k7enxv4jsxqzpuaztrnwngzn3kdzw5hydlzf03qdgm2hdq27cqv3agm2awhz5se903vruatfhq77w3ls4evs3ch9zw97j25emudupq63nyw24cg27h2rspfj9srp"
            )
        )
        Assert.assertEquals("lnbc", bolt11.prefix)
        Assert.assertEquals(MilliSat(250000000), bolt11.amount)
        Assert.assertEquals(1496314658, bolt11.timestampSeconds)

        Assert.assertEquals(3, bolt11.tags.size)
        Assert.assertEquals(Bolt11.TaggedField.PaymentHash.tag, bolt11.tags[0].tag)
        Assert.assertEquals(Bolt11.TaggedField.Description.tag, bolt11.tags[1].tag)
        Assert.assertEquals(Bolt11.TaggedField.Expiry.tag, bolt11.tags[2].tag)

        Assert.assertEquals(1.toByte(), bolt11.recoveryFlag)
        Assert.assertEquals(
            "e89639ba6814e36689d4b91bf125f10351b55da057b00647a8dabaeb8a90c95f160f9d5a6e0f79d1fc2b964238b944e2fa4aa677c6f020d466472ab842bd750e",
            bolt11.signature
        )
        Assert.assertEquals("fj9srp", bolt11.checksum)
    }

    @Test
    fun `Decode upper cased payment request`() {
        val bolt11 = Bolt11.decode(
            LightningPaymentRequest(
                "LNBC1PVJLUEZPP5QQQSYQCYQ5RQWZQFQQQSYQCYQ5RQWZQFQQQSYQCYQ5RQWZQFQYPQDPL2PKX2CTNV5SXXMMWWD5KGETJYPEH2URSDAE8G6TWVUS8G6RFWVS8QUN0DFJKXAQ8RKX3YF5TCSYZ3D73GAFNH3CAX9RN449D9P5UXZ9EZHHYPD0ELX87SJLE52X86FUX2YPATGDDC6K63N7ERQZ25LE42C4U4ECKY03YLCQCA784W"
            )
        )
        Assert.assertEquals("lnbc", bolt11.prefix)
        Assert.assertNull(bolt11.amount)
        Assert.assertEquals(1496314658,bolt11.timestampSeconds)

        Assert.assertEquals(2, bolt11.tags.size)
        Assert.assertEquals(Bolt11.TaggedField.PaymentHash.tag, bolt11.tags.first().tag)
        Assert.assertEquals(Bolt11.TaggedField.Description.tag, bolt11.tags.last().tag)
        Assert.assertEquals(
            "38ec6891345e204145be8a3a99de38e98a39d6a569434e1845c8af7205afcfcc7f425fcd1463e93c32881ead0d6e356d467ec8c02553f9aab15e5738b11f127f",
            bolt11.signature
        )
        Assert.assertEquals(0.toByte(), bolt11.recoveryFlag)
        Assert.assertEquals("ca784w", bolt11.checksum)
    }
}