package chat.sphinx.concept_coredb

import chat.sphinx.conceptcoredb.SphinxDatabaseQueries

abstract class SphinxCoreDB {

    /**
     * Suspends until [SphinxDatabaseQueries] is initialized. Initialization occurs
     * after decryption and retrieval of the user's encryption key, which allows
     * for the decrypting of the DB.
     * */
    abstract suspend fun getSphinxDatabaseQueries(): SphinxDatabaseQueries
}
