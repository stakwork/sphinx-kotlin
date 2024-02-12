package chat.sphinx.example.wrapper_mqtt

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class TribeMember(
    val pubkey: String?,
    val alias: String?,
    val photo_url: String?,
    val person: String?,
    val route_hint: String?,
    val contact_key: String?
)

@JsonClass(generateAdapter = true)
data class TribeMembersResponse(
    val confirmed: List<TribeMember>?,
    val pending: List<TribeMember>?
) {
    companion object {
        fun String.toTribeMembersList(moshi: Moshi): TribeMembersResponse? {
            val adapter = moshi.adapter(TribeMembersResponse::class.java)
            return try {
                adapter.fromJson(this)
            } catch (e: Exception) {
                null
            }
        }
    }
}
