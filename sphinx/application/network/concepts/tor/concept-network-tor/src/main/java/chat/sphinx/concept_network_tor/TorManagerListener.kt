package chat.sphinx.concept_network_tor

interface TorManagerListener {

    /**
     * t/f if setting has been persisted, otherwise `null`
     * */
    suspend fun onTorRequirementChange(required: Boolean)

    suspend fun onTorSocksProxyAddressChange(socksProxyAddress: SocksProxyAddress?)
}
