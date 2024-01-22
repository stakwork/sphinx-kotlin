package chat.sphinx.example.concept_connect_manager.model

data class OwnerInfo(
    val alias: String,
    val picture: String,
    val userState: String?,
    val messageLastIndex: Long?
)