package chat.sphinx.feature_repository_android

import chat.sphinx.concept_coredb.CoreDB
import chat.sphinx.concept_crypto_rsa.RSA
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.feature_repository.SphinxRepository
import chat.sphinx.logger.SphinxLogger
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_authentication.data.AuthenticationStorage
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.feature_authentication_core.AuthenticationCoreManager

class SphinxRepositoryAndroid(
    authenticationCoreManager: AuthenticationCoreManager,
    authenticationStorage: AuthenticationStorage,
    coreDB: CoreDB,
    dispatchers: CoroutineDispatchers,
    moshi: Moshi,
    networkQueryChat: NetworkQueryChat,
    networkQueryContact: NetworkQueryContact,
    networkQueryLightning: NetworkQueryLightning,
    networkQueryMessage: NetworkQueryMessage,
    rsa: RSA,
    socketIOManager: SocketIOManager,
    LOG: SphinxLogger,
): SphinxRepository(
    authenticationCoreManager,
    authenticationStorage,
    coreDB,
    dispatchers,
    moshi,
    networkQueryChat,
    networkQueryContact,
    networkQueryLightning,
    networkQueryMessage,
    rsa,
    socketIOManager,
    LOG,
) {
}
