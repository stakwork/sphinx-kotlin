package chat.sphinx.concept_network_query_lightning.model.invoice

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeatureDto(
    val name: String,
    val is_required: Boolean,
    val is_known: Boolean,
)
