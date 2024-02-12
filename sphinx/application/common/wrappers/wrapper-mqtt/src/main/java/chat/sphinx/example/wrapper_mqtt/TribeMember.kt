package chat.sphinx.example.wrapper_mqtt

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Types

@JsonClass(generateAdapter = true)
data class TribeMember(
    val pubkey: String?,
    val alias: String?,
    val photo_url: String?,
    val person: String?,
    val confirmed: Boolean?,
    val route_hint: String?,
    val contact_key: String?
) {

    companion object {
    fun String.toTribeMembersList(moshi: Moshi): List<TribeMember>? {
        val type = Types.newParameterizedType(List::class.java, TribeMember::class.java)
        val adapter = moshi.adapter<List<TribeMember>>(type)
        return try {
            adapter.fromJson(this)
        } catch (e: Exception) {
            null
        }
    }
}
}

