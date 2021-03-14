package chat.sphinx.concept_network_query_chat.model

@Suppress("PropertyName")
abstract class BaseChatDto<IsMuted, Seen> {
    abstract val id: Long
    abstract val uuid: String
    abstract val name: String?
    abstract val photo_url: String?
    abstract val type: Int
    abstract val status: Int?
    abstract val contact_ids: List<Long>
    abstract val is_muted: IsMuted // from '/msgs' endpoint, this comes in as a boolean while the '/chats' is an Int
    abstract val created_at: String
    abstract val updated_at: String
    abstract val deleted: Int
    abstract val group_key: String?
    abstract val host: String?
    abstract val price_to_join: Long?
    abstract val price_per_message: Long?
    abstract val escrow_millis: Long?
    abstract val unlisted: Int
    abstract val private: Int?
    abstract val owner_pub_key: String?
    abstract val seen: Seen // from '/msgs' endpoint, this comes in as a boolean while the '/chats' is an Int
    abstract val app_url: String?
    abstract val feed_url: String?
    abstract val meta: String?
    abstract val my_photo_url: String?
    abstract val my_alias: String?
    abstract val tenant: Int
    abstract val skip_broadcast_joins: Int?
    abstract val pending_contact_ids: List<Long>?
}
