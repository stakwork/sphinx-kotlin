package chat.sphinx.feature_repository.mappers.contact

import chat.sphinx.conceptcoredb.ContactDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_contact.Contact
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class ContactDboPresenterMapper(
    dispatchers: CoroutineDispatchers
): ClassMapper<ContactDbo, Contact>(dispatchers) {
    override suspend fun mapFrom(value: ContactDbo): Contact {
        TODO("Not yet implemented")
    }

    override suspend fun mapTo(value: Contact): ContactDbo {
        TODO("Not yet implemented")
    }
}
