package chat.sphinx.concept_network_query_lightning.model.channel

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChannelsDto(val channels: List<ChannelDto>)