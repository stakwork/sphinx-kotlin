package chat.sphinx.concept_network_query_lightning.model.log

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LogsDto (val logs: String)