package chat.sphinx.example.wrapper_mqtt

import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.ShortChannelId
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toShortChannelId
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

fun String.toLspChannelInfo(moshi: Moshi): LspChannelInfo? {
    return try {
        moshi.adapter(LspChannelInfoMoshi::class.java).fromJson(this)?.let {
            LspChannelInfo(
                it.scid.toShortChannelId(),
                it.server_pubkey.toLightningNodePubKey()
            )
        }
    } catch (e: Exception) {
        null
    }
}

data class LspChannelInfo(
    val scid: ShortChannelId?,
    val serverPubKey: LightningNodePubKey?
)

@JsonClass(generateAdapter = true)
data class LspChannelInfoMoshi(
    val scid: String,
    val server_pubkey: String
)