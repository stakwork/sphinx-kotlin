package chat.sphinx.concept_repository_chat.model

import chat.sphinx.concept_network_query_chat.model.TribeMemberDto
import chat.sphinx.wrapper_common.lightning.isValidLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.isValidLightningRouteHint
import java.io.File

class AddMember private constructor(
    val alias: String,
    val publicKey: String,
    val routeHint: String?,
    val contactKey: String,
    val img: File?,
    val photoUrl: String?,
    val chatId: Long,
) {

    enum class ValidationError {
        MISSING_FIELDS, INVALID_PUBLIC_KEY, INVALID_ROUTE_HINT
    }

    class Builder {
        private var alias: String? = null
        private var publicKey: String? = null
        private var routeHint: String? = null
        private var contactKey: String? = null
        private var img: File? = null
        private var photoUrl: String? = null
        private var chatId: Long? = null

        @get:Synchronized
        val hasRequiredFields: Boolean
            get() {
                return !alias.isNullOrEmpty() &&
                        !publicKey.isNullOrEmpty() &&
                        !contactKey.isNullOrEmpty() &&
                        chatId != null
            }

        @get:Synchronized
        val isImageSet: Boolean
            get() {
                return img != null
            }

        @Synchronized
        fun setAlias(alias: String?): Builder {
            this.alias = alias
            return this
        }

        @Synchronized
        fun setPhotoUrl(photoUrl: String?): Builder {
            this.photoUrl = photoUrl
            return this
        }

        @Synchronized
        fun setPublicKey(publicKey: String?): Builder {
            this.publicKey = publicKey
            return this
        }

        @Synchronized
        fun setRouteHint(routeHint: String?): Builder {
            this.routeHint = routeHint
            return this
        }

        @Synchronized
        fun setContactKey(contactKey: String?): Builder {
            this.contactKey = contactKey
            return this
        }

        @Synchronized
        fun setImg(img: File?): Builder {
            this.img?.let {
                try {
                    it.delete()
                } catch (e: Exception) {}
            }
            this.img = img
            return this
        }

        @Synchronized
        fun setChatId(chatId: Long?): Builder {
            this.chatId = chatId
            return this
        }

        @Synchronized
        fun build(): Pair<AddMember?, ValidationError?> {
            return  if (!hasRequiredFields) {
                Pair(null, ValidationError.MISSING_FIELDS)
            } else if (publicKey != null && !publicKey!!.isValidLightningNodePubKey) {
                Pair(null, ValidationError.INVALID_PUBLIC_KEY)
            } else if (routeHint != null && routeHint!!.isNotEmpty() && !routeHint!!.isValidLightningRouteHint) {
                Pair(null, ValidationError.INVALID_ROUTE_HINT)
            } else {
                Pair(
                    AddMember(
                        alias = alias!!,
                        publicKey = publicKey!!,
                        routeHint = routeHint,
                        photoUrl = photoUrl,
                        img = img,
                        contactKey = contactKey!!,
                        chatId = chatId!!
                    ),
                    null
                )
            }
        }

    }

    fun toTribeMemberDto(imageUrl: String? = null): TribeMemberDto {
        return TribeMemberDto(
            chat_id = chatId,
            alias = alias,
            photo_url = imageUrl ?: photoUrl,
            pub_key = publicKey,
            route_hint = routeHint,
            contact_key = contactKey
        )
    }
}