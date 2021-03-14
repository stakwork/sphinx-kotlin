package chat.sphinx.concept_network_query_chat.model

@Suppress("PropertyName")
abstract class BaseChatDto<IsMuted, Unlisted, Private, Seen> {
    abstract val id: Long
    abstract val uuid: String
    abstract val name: String?
    abstract val photo_url: String?
    abstract val type: Int
    abstract val status: Int?
    abstract val contact_ids: List<Long>

    // from '/msgs' endpoint this comes in as a nullable boolean
    // while the '/chats' and '/contacts' endpoints it is a nullable Int
    abstract val is_muted: IsMuted

    abstract val created_at: String
    abstract val updated_at: String
    abstract val deleted: Int
    abstract val group_key: String?
    abstract val host: String?
    abstract val price_to_join: Long?
    abstract val price_per_message: Long?
    abstract val escrow_millis: Long?

    // from '/msgs' endpoint, this comes in as a boolean while the
    // '/chats' and '/contacts' endpoints it is an Int
    abstract val unlisted: Unlisted

    // from '/msgs' endpoint this comes in as a nullable boolean
    // while the '/chats' and '/contacts' endpoints it is a nullable Int
    abstract val private: Private

    abstract val owner_pub_key: String?

    // from '/msgs' endpoint, this comes in as a boolean while the
    // '/chats' and '/contacts' endpoints it is an Int
    abstract val seen: Seen

    abstract val app_url: String?
    abstract val feed_url: String?
    abstract val meta: String?
    abstract val my_photo_url: String?
    abstract val my_alias: String?
    abstract val tenant: Int
    abstract val skip_broadcast_joins: Int?
    abstract val pending_contact_ids: List<Long>?
}
