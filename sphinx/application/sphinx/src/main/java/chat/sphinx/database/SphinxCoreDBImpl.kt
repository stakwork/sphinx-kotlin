package chat.sphinx.database

import android.content.Context
import chat.sphinx.concept_coredb.SphinxDatabase
import chat.sphinx.feature_coredb.CoreDBImpl
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.concept_encryption_key.EncryptionKey
import io.matthewnelson.crypto_common.annotations.RawPasswordAccess
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

class SphinxCoreDBImpl(
    context: Context,
    private val buildConfigDebug: BuildConfigDebug,
    moshi: Moshi,
): CoreDBImpl(moshi) {

    private val appContext: Context = context.applicationContext

    @Volatile
    private var driver: AndroidSqliteDriver? = null

    override fun getSqlDriver(encryptionKey: EncryptionKey): SqlDriver {
        return driver ?: synchronized(this) {
            driver ?: createSqlDriver(encryptionKey)
                .also { driver = it }
        }
    }

    private fun createSqlDriver(encryptionKey: EncryptionKey): AndroidSqliteDriver {
        // Don't encrypt the DB for debug version
        return if (buildConfigDebug.value) {
            AndroidSqliteDriver(
                SphinxDatabase.Schema,
                appContext,
                DB_NAME
            )
        } else {
            @OptIn(RawPasswordAccess::class)
            val passphrase: ByteArray = SQLiteDatabase.getBytes(encryptionKey.privateKey.value)

            @Suppress("RedundantExplicitType")
            val factory: SupportFactory = SupportFactory(passphrase, null, true)

            AndroidSqliteDriver(
                SphinxDatabase.Schema,
                appContext,
                DB_NAME,
                factory
            )
        }
    }
}
