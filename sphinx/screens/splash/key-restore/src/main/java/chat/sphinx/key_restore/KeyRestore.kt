package chat.sphinx.key_restore

import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import io.matthewnelson.k_openssl_common.clazzes.Password
import kotlinx.coroutines.flow.Flow

abstract class KeyRestore {

    /**
     * Used only by the Splash screen for restoring keys, and implemented by the application
     * such that only those 2 modules have knowledge of this functionality.
     * */
    abstract fun restoreKeys(
        privateKey: Password,
        publicKey: Password,
        userPin: CharArray,
        relayUrl: RelayUrl,
        authorizationToken: AuthorizationToken
    ): Flow<KeyRestoreResponse>
}