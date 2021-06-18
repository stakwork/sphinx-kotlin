package chat.sphinx.feature_network_query_message.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostBoostMessage(
    val boost: Boolean = true,
    val text: String = "",
    val chat_id: Long,
    val amount: Long,
    val message_price: Long,
    val reply_uuid: String,
) {

    companion object {
        const val REQUIRE_MESSAGE = "PostBoostMessage Dto field"
    }

    init {
        require(boost) {
            "$REQUIRE_MESSAGE boost must be true"
        }
        require(text.isEmpty()) {
            "$REQUIRE_MESSAGE text must be empty"
        }
        require(amount > 0) {
            "$REQUIRE_MESSAGE amount must be greater than 0"
        }
        require(message_price >= 0) {
            "$REQUIRE_MESSAGE message_price must be greater than or equal to 0"
        }
        require(reply_uuid.isNotEmpty()) {
            "$REQUIRE_MESSAGE reply_uuid must not be empty"
        }
    }
}
