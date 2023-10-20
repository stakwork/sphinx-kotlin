package chat.sphinx.concept_network_query_people.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TribeMemberProfileDto(
    val id: Int,
    val description: String,
    val img: String,
    val owner_alias: String,
    val owner_contact_key: String,
    val owner_route_hint: String,
    val price_to_meet: Int,
    val unique_name: String,
    val uuid: String,
    val extras: TribeMemberProfileExtrasDto?,
)

@JsonClass(generateAdapter = true)
data class TribeMemberProfileExtrasDto(
    val coding_languages: List<TribeMemberProfileCodingLanguageDto>?,
    val github: List<ProfileAttributeDto>?,
    val twitter: List<ProfileAttributeDto>?,
    val post: List<TribeMemberProfilePostDto>?,
    val tribes: List<ProfileAttributeDto>?,
) {
    val codingLanguages: String
        get() {
            if (coding_languages?.isNotEmpty() == true) {
                return coding_languages?.joinToString(",") {
                    it.value
                } ?: ""
            }
            return "-"
        }
}

@JsonClass(generateAdapter = true)
data class TribeMemberProfileCodingLanguageDto(
    val label: String,
    val value: String
)

@JsonClass(generateAdapter = true)
data class TribeMemberProfilePostDto(
    val content: String,
    val title: String,
    val created: Int
)

@JsonClass(generateAdapter = true)
data class ProfileAttributeDto(
    val value: String
) {
    val formattedValue: String
        get() {
            if (value.isEmpty()) {
                return "-"
            }
            return value
        }
}