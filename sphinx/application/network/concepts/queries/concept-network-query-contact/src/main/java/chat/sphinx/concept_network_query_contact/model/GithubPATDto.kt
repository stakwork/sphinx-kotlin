package chat.sphinx.concept_network_query_contact.model

import com.squareup.moshi.JsonClass

/**
 * Only non-null fields will be serialized to Json for the request body.
 * */
@JsonClass(generateAdapter = true)
data class GithubPATDto(
    val encrypted_pat: String? = null,
)
