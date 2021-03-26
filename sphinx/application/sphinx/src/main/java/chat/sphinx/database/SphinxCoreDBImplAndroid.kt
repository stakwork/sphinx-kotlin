package chat.sphinx.database

import android.content.Context
import chat.sphinx.concept_coredb.SphinxDatabase
import chat.sphinx.feature_coredb.SphinxCoreDBImpl
import com.squareup.sqldelight.android.AndroidSqliteDriver
import dagger.hilt.android.qualifiers.ApplicationContext
import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.k_openssl_common.annotations.RawPasswordAccess
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SphinxCoreDBImplAndroid @Inject constructor(
    @ApplicationContext private val appContext: Context,
): SphinxCoreDBImpl() {

    @OptIn(RawPasswordAccess::class)
    override fun initializeDatabase(encryptionKey: EncryptionKey) {
        val passphrase: ByteArray = SQLiteDatabase.getBytes(encryptionKey.privateKey.value)
        val factory = SupportFactory(passphrase, null, true)
        val driver = AndroidSqliteDriver(
            SphinxDatabase.Schema,
            appContext,
            DB_NAME,
            factory
        )
        setDatabaseQueries(driver)
    }
}
