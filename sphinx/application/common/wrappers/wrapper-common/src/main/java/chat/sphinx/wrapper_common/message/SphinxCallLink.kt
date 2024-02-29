package chat.sphinx.wrapper_common.message

import com.squareup.moshi.Moshi
import java.net.MalformedURLException
import java.net.URL

@Suppress("NOTHING_TO_INLINE")
inline fun String.toSphinxCallLink(): SphinxCallLink? =
    try {
        SphinxCallLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidSphinxCallLink: Boolean
    get() = isNotEmpty() && matches("^${SphinxCallLink.REGEX}\$".toRegex())

inline val String.isValidJitsiCallLink: Boolean
    get() = isNotEmpty() && startsWith(SphinxCallLink.DEFAULT_CALL_SERVER_URL)

@JvmInline
value class SphinxCallLink(val value: String) {

    companion object {
        const val REGEX = "https:\\/\\/.*\\/sphinx\\.call\\..*"

        const val CALL_SERVER_URL_KEY = "meeting-server-url"
        const val DEFAULT_CALL_SERVER_URL = "https://jitsi.sphinx.chat"
        private const val CALL_ROOM_NAME = "sphinx.call"

        const val AUDIO_ONLY_PARAM = "config.startAudioOnly"

        fun newCallLink(
            customServerUrl: String?,
            startAudioOnly: Boolean
        ): String? {
            val currentTime = System.currentTimeMillis()
            val audioOnlyParam = if (startAudioOnly) "#${AUDIO_ONLY_PARAM}=true" else ""
            val linkString = "${customServerUrl ?: DEFAULT_CALL_SERVER_URL}/$CALL_ROOM_NAME.$currentTime$audioOnlyParam"

            return linkString.toSphinxCallLink()?.value
        }

        fun newCallLinkMessage(
            customServerUrl: String?,
            startAudioOnly: Boolean,
            moshi: Moshi
        ): String? {
            val currentTime = System.currentTimeMillis()
            val audioOnlyParam = if (startAudioOnly) "#${AUDIO_ONLY_PARAM}=true" else ""
            val linkString = "${customServerUrl ?: DEFAULT_CALL_SERVER_URL}/$CALL_ROOM_NAME.$currentTime$audioOnlyParam"

            linkString.toSphinxCallLink()?.let { sphinxCallLink ->
                val callLinkMessage = CallLinkMessage(
                    sphinxCallLink,
                    false,
                    ""
                )

                callLinkMessage.toJson(moshi)?.let { jsonLink ->
                    return "${CallLinkMessage.MESSAGE_PREFIX}$jsonLink"
                }
            }
            return null
        }
    }

    init {
        require(value.isValidSphinxCallLink || value.isValidJitsiCallLink) {
            "Invalid Sphinx Call Link"
        }
    }

    inline val startAudioOnly : Boolean
        get() = getParameter(AUDIO_ONLY_PARAM).toBoolean()

    inline val callServer : String
        get() = value.substringBefore("sphinx.call")

    inline val callRoom : String
        get() = "sphinx.call." + value.substringAfter("sphinx.call.").substringBefore("#").substringBefore("?")

    inline val callServerUrl : URL?
        get() {
            return try {
                URL(callServer)
            } catch (e: MalformedURLException) {
                null
            }
        }

    fun getParameter(k: String): String? {
        val parameters = value.substringAfter("#").split("&")
        for (parameter in parameters) {
            val paramComponents = parameter.split("=")
            val key:String? = if (paramComponents.isNotEmpty()) paramComponents.elementAtOrNull(0) else null
            val value:String? = if (paramComponents.size > 1) paramComponents.elementAtOrNull(1) else null

            if (key == k) return value
        }
        return null
    }

}