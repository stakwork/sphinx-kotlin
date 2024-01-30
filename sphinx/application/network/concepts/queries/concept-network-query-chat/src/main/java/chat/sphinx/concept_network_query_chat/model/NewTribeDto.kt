package chat.sphinx.concept_network_query_chat.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.File

@JsonClass(generateAdapter = true)
data class NewTribeDto(
    val name: String,
    val pubkey: String,
    val route_hint: String,
    val member_count: Int?,
    val last_active: Int?,
    val created: Long?,
    val updated: Long?
) {

    var amount: Long? = null
    var host: String? = null
    var uuid: String? = null
    var price_to_join: Long = 0


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
            } catch (e: Exception) {
            }
        }
        this.profileImgFile = img
    }


    fun set(
        host: String?,
        tribePubKey: String,
    ) {
        this.host = host
        this.uuid = tribePubKey
    }
}