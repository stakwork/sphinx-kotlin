package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.File

@JsonClass(generateAdapter = true)
data class TribeDto(
    val name: String,
    val description: String,
    val img: String?,
    val tags: Array<String> = arrayOf(),
    val group_key: String,
    val owner_pubkey: String,
    val owner_route_hint: String?,
    val owner_alias: String?,
    val price_to_join: Long = 0,
    val price_per_message: Long = 0,
    val escrow_amount: Long = 0,
    val escrow_millis: Long = 0,
    val unlisted: Boolean?,
    val private: Any?,
    val deleted: Any?,
    val app_url: String?,
    val feed_url: String?,
    val feed_type: Int?,
    val pin: String?,
    var badges: Array<String> = arrayOf()
) {

    var amount: Long? = null
    var host: String? = null
    var uuid: String? = null

    var joined: Boolean? = null

    @Json(name = "my_alias")
    var myAlias: String? = null

    @Json(name = "my_photo_url")
    var myPhotoUrl: String? = null

    @Transient
    var profileImgFile: File? = null

    fun setProfileImageFile(img: File?) {
        this.profileImgFile?.let {
            try {
                it.delete()
            } catch (e: Exception) {}
        }
        this.profileImgFile = img
    }

    val hourToStake: Long
        get() = (escrow_millis) / 60 / 60 / 1000

    fun set(
        host: String?,
        tribePubKey: String,
    ) {
        this.host = host
        this.uuid = tribePubKey
    }
}
