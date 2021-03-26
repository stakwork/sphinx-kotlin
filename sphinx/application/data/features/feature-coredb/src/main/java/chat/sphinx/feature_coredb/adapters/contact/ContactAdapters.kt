package chat.sphinx.feature_coredb.adapters.contact

import chat.sphinx.wrapper_common.contact.ContactId
import com.squareup.sqldelight.ColumnAdapter


internal class ContactIdsAdapter: ColumnAdapter<List<ContactId>, String> {
    override fun decode(databaseValue: String): List<ContactId> {
        if (databaseValue.isEmpty()) {
            return listOf()
        }

        return databaseValue.split(",").map { ContactId(it.toLong()) }
    }

    override fun encode(value: List<ContactId>): String {
        return value.joinToString(",") { it.value.toString() }
    }
}
