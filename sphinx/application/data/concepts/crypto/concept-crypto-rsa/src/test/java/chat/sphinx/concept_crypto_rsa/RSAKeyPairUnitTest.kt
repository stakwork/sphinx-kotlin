package chat.sphinx.concept_crypto_rsa

import chat.sphinx.wrapper_rsa.PKCSType
import chat.sphinx.wrapper_rsa.formatToCert
import chat.sphinx.wrapper_rsa.toRsaPrivateKey
import chat.sphinx.wrapper_rsa.toRsaPublicKey
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import org.junit.Assert
import org.junit.Test

@OptIn(RawPasswordAccess::class)
class RSAKeyPairUnitTest {

    companion object {
        // Note... test data will not be trimmed such that we can ensure
        // that the extension functions are properly formatted on output

        const val RSA_PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC9QjbU+Ae2Ihlj
            P8wnreUMNBHtxFrVSpCSKRnrAUm0T6XgsSYMvlu73nClI/6TgTxhYoc4buGkrLQu
            +lvaJLt8O3x/Q+po1nWjNQPZGDpIuJf6qQWJAn/Mnc3zkV8k76WhYsihkofhDEaj
            cXnK3IPZQiJWQIL+ttnNzyBxnY0EhYHAfh43KNED4rElQjnTHlQnHeDitt/tqqaW
            TuGpjX8tsUXeSnLMRUJzT44P0KVY5rcr2hr12+djr2olftHv4SbBpJZkdV51KR8u
            BG09r5O0tdWNpmHZhcPm1VorTo4ynJJ0bl/2qQxq7MMcxpRbu4276MkUSG92iuNl
            OCcZStT/AgMBAAECggEAYKEPB9t/bHGqjq8DGHtDx+BKCyDG08HXYJjVn9Qvgn1s
            DvXDnJwQkDI5R0fCciN45av9qEWMtZxr/tRa010P0JU6smvFojNlKqglx9ED6R8+
            kX980QBSqfEkNyjQ8DXfVi/uifgboj63Tjng2j+Onf2TNICJoW1QdTE8umWbm2N/
            5wFtspGbX8HaBkc2Kd+BxVDzOXjxRDQMpZA9lr2TcDC+vObVzic/3wELGIUtUAOj
            Z7pHCaR2X6AdlfCeENA/uuMUp91wubvZNk+TkQCa2gbg+TodCBP65khaN31wzR+U
            +dSx6ZclIWe7rytui7GhsYN50961hfR92yGhuZEh2QKBgQD9UvqxqqOezKzcWGmx
            C7YOMjWfD5dvfEcwQUFEQNfq/HiHd2G+AaWsqbXtHbKhsOzS1wGob7/WUVgRuPli
            PzYbagGCqNajhKekAr0c7lJnj/O9sGs4tpxph1yckij1+PlGiicCBp1S4FGwN++A
            UqVfpgbtfCeBv9PcXef8K0ptLQKBgQC/Qf5hcqDFC4nSgQSZU6WqSg3TWGttjrgI
            wRbss3SRzKTo7WGFR99eaOlHgzAMWoFruw24kQ3QNVbifuGgjUQVr154GFxusu23
            SXo82ZCZ/sBnBHp/s9qaRvbrdS0HZKO3cUsMhEr9kyr9iqM1bglHKdGVB3r9VNTx
            cPFOy23eWwKBgFAzzlUjrkvfhzb38vZvu8MA2IM0f4B3e15mupAua23lYw+Yl2R9
            xwNEc+nPhje0+TXDhq3aO4VSGaz6wFH/q1TAmhvQ8quwmxbNGyyms4ZASC4dRcUd
            46Vxiruzdq5xlhJkOPdmOrb7eEFvZC9feyYuPXFA5o0ou53ASWtIFXc9AoGBAKEs
            /y+mBwAnKh1gpryDtl+ciqwpnla0aDjQZ/5PeRQJbovwyDcmi4GnTbf7QTmX5+se
            toBslUR6Pt8e6AuCIPdZU3ZHqVWMqjHE1SOEgYyVGBV4u7b0nWtYFGv20lyhnipQ
            aluA/SVWqWsDM2kqct0qy7kYlbf5jn5tnJ23Z75zAoGBAPUopHOxzzT90ADJoKqY
            CUBkYaz8SnKmcEhEEnwBO9c07VkglV7mR4Q4afz7x8N3Zot05dXDNorntMZoxxqv
            MoK4v4NfwpYnk8Y2nTMRRmAE+MRu6Z+FvF9JK1GEWXzT12orA7O0eAoUaJ+ul7Cb
            HprGSrMI6uawk/RN/hm1VlLK
            -----END PRIVATE KEY-----
        """

        const val RSA_PUB_KEY = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvUI21PgHtiIZYz/MJ63l
            DDQR7cRa1UqQkikZ6wFJtE+l4LEmDL5bu95wpSP+k4E8YWKHOG7hpKy0Lvpb2iS7
            fDt8f0PqaNZ1ozUD2Rg6SLiX+qkFiQJ/zJ3N85FfJO+loWLIoZKH4QxGo3F5ytyD
            2UIiVkCC/rbZzc8gcZ2NBIWBwH4eNyjRA+KxJUI50x5UJx3g4rbf7aqmlk7hqY1/
            LbFF3kpyzEVCc0+OD9ClWOa3K9oa9dvnY69qJX7R7+EmwaSWZHVedSkfLgRtPa+T
            tLXVjaZh2YXD5tVaK06OMpySdG5f9qkMauzDHMaUW7uNu+jJFEhvdorjZTgnGUrU
            /wIDAQAB
            -----END PUBLIC KEY-----
        """
    }

    @Test
    fun `String value conversion to RsaPrivateKey contains no spaces`() {
        val privKey = RSA_PRIVATE_KEY.toRsaPrivateKey().value
        Assert.assertFalse(privKey.contains(' '))
    }

    @Test
    fun `String value conversion to RsaPublicKey contains no spaces`() {
        val pubKey = RSA_PUB_KEY.toRsaPublicKey().value
        Assert.assertFalse(pubKey.contains(' '))
    }

    @Test
    fun `PrivateKey's formatToCert (multi-line) returns the same Cert`() {
        val privKey = RSA_PRIVATE_KEY.toRsaPrivateKey()
        val privKeyBackToCert = privKey.formatToCert(singleLine = false, pkcsType = PKCSType.PKCS8)

        // the test data constants are untrimmed
        Assert.assertNotEquals(RSA_PRIVATE_KEY, privKeyBackToCert)

        Assert.assertEquals(RSA_PRIVATE_KEY.trimIndent(), privKeyBackToCert)
    }

    @Test
    fun `PublicKey's formatToCert (multi-line) returns the same Cert`() {
        val pubKey = RSA_PUB_KEY.toRsaPublicKey()
        val pubKeyBackToCert = pubKey.formatToCert(singleLine = false, pkcsType = PKCSType.PKCS8)

        // the test data constants are untrimmed
        Assert.assertNotEquals(RSA_PUB_KEY, pubKeyBackToCert)

        Assert.assertEquals(RSA_PUB_KEY.trimIndent(), pubKeyBackToCert)
    }
}