package chat.sphinx.concept_network_query_crypter.model

class SendSeedDto {
    var seed: String? = null
    var ssid: String? = null
    var pass: String? = null
    var lightningNodeIP: String? = null
    var lightningNodePort: String? = null
    var pubkey: String? = null
    var network: String = "regtest"
}