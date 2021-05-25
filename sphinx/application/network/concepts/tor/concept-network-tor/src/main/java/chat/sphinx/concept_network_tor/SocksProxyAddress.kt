package chat.sphinx.concept_network_tor

inline val SocksProxyAddress.host: String
    get() = value.split(':')[0]

inline val SocksProxyAddress.port: Int
    get() = value.split(':')[1].toInt()

@JvmInline
value class SocksProxyAddress(val value: String)
