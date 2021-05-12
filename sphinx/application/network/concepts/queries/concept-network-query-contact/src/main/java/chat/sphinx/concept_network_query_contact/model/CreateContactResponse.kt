package chat.sphinx.concept_network_query_contact.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateContactResponse(
    val contact: ContactDto,
)
//02c8d9068ecfd41c9114a4a5a25036f39071e2c75e6db75f13521e6db393facbd0