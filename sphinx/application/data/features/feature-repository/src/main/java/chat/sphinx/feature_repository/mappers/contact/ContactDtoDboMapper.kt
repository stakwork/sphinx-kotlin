package chat.sphinx.feature_repository.mappers.contact

import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.conceptcoredb.ContactDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import io.matthewnelson.concept_coroutines.CoroutineDispatchers

internal class ContactDtoDboMapper(
    dispatchers: CoroutineDispatchers
): ClassMapper<ContactDto, ContactDbo>(dispatchers) {
    override suspend fun mapFrom(value: ContactDto): ContactDbo {
        TODO("Not yet implemented")
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun mapTo(value: ContactDbo): ContactDto {
        throw IllegalArgumentException("Going from a ContactDbo to ContactDto is not allowed")
    }
}
