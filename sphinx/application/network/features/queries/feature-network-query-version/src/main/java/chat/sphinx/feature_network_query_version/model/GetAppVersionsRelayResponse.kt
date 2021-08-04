package chat.sphinx.feature_network_query_version.model

import chat.sphinx.concept_network_query_version.model.AppVersionsDto
import chat.sphinx.concept_network_relay_call.RelayResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetAppVersionsRelayResponse(
    override val success: Boolean,
    override val response: AppVersionsDto?,
    override val error: String?
): RelayResponse<AppVersionsDto>()