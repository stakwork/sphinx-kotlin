package chat.sphinx.concept_coredb

import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import kotlinx.coroutines.CancellationException

abstract class SphinxCoreDB {

    /**
     * Suspends until [SphinxDatabaseQueries] is initialized. Initialization occurs
     * after decryption and retrieval of the user's encryption key, which allows
     * for the decrypting of the DB.
     * */
    @Throws(CancellationException::class)
    abstract suspend fun getSphinxDatabaseQueries(): SphinxDatabaseQueries
}
