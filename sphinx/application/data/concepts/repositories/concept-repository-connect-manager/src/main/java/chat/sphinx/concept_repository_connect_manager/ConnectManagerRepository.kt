package chat.sphinx.concept_repository_connect_manager

interface ConnectManagerRepository {

//    val pim = "tcp://54.164.163.153:1883"

    fun setLspIp(lspIp: String)
    fun createOwnerAccount()

    suspend fun persistAndShowMnemonic(words: String) {}
    fun createOwnerWithOkKey(okKey: String) {}
    fun updateLspAndOwner(data: String) {}


}