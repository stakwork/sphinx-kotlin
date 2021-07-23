package chat.sphinx.concept_repository_chat.model

import chat.sphinx.concept_network_query_chat.model.PostGroupDto
import chat.sphinx.wrapper_chat.AppUrl
import chat.sphinx.wrapper_chat.FeedUrl
import chat.sphinx.wrapper_chat.toAppUrl
import chat.sphinx.wrapper_chat.toFeedUrl
import java.io.File

class CreateTribe private constructor(
    val name: String,
    val description: String,
    val isTribe: Boolean? = true,
    val pricePerMessage: Long? = 0L,
    val priceToJoin: Long? = 0L,
    val escrowAmount: Long? = 0L,
    val escrowMillis: Long? = 0L,
    val img: File? = null,
    val tags: Array<String> = arrayOf(),
    val unlisted: Boolean? = false,
    val private: Boolean? = false,
    val appUrl: AppUrl? = null,
    val feedUrl: FeedUrl? = null,
) {

    class Builder(
        val tags: Array<Tag>
    ) {
        class Tag (
            val name: String,
            val image: Int,
            var isSelected: Boolean = false
        ) {
            override fun toString(): String {
                return name
            }
        }

        private var name: String? = null
        private var description: String? = null
        private var isTribe: Boolean? = true
        private var pricePerMessage: Long? = 0L
        private var priceToJoin: Long? = 0L
        private var escrowAmount: Long? = 0L
        private var escrowMillis: Long? = 0L
        private var img: File? = null
        private var unlisted: Boolean? = false
        private var private: Boolean? = false
        private var appUrl: AppUrl? = null
        private var feedUrl: FeedUrl? = null

        @get:Synchronized
        val hasRequiredFields: Boolean
            get() = !name.isNullOrEmpty() && !description.isNullOrEmpty()

        @Synchronized
        fun setName(name: String?): Builder {
            this.name = name
            return this
        }

        @Synchronized
        fun setDescription(description: String?): Builder {
            this.description = description
            return this
        }

        @Synchronized
        fun setIsTribe(isTribe: Boolean?): Builder {
            this.isTribe = isTribe
            return this
        }

        @Synchronized
        fun setPricePerMessage(pricePerMessage: Long?): Builder {
            this.pricePerMessage = pricePerMessage
            return this
        }

        @Synchronized
        fun setPriceToJoin(priceToJoin: Long?): Builder {
            this.priceToJoin = priceToJoin
            return this
        }
        @Synchronized
        fun setEscrowAmount(escrowAmount: Long?): Builder {
            this.escrowAmount = escrowAmount
            return this
        }

        @Synchronized
        fun setEscrowMillis(escrowMillis: Long?): Builder {
            this.escrowMillis = escrowMillis
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
        fun selectTag(index: Int, isSelected: Boolean): Builder {
            this.tags[index].isSelected = isSelected
            return this
        }

        fun toggleTag(index: Int): Builder {
            this.tags[index].isSelected = !this.tags[index].isSelected
            return this
        }
        @Synchronized
        fun setUnlisted(unlisted: Boolean?): Builder {
            this.unlisted = unlisted
            return this
        }

        @Synchronized
        fun setPrivate(private: Boolean?): Builder {
            this.private = private
            return this
        }
        @Synchronized
        fun setAppUrl(appUrl: String?): Builder {
            this.appUrl = appUrl?.toAppUrl()
            return this
        }

        @Synchronized
        fun setFeedUrl(feedUrl: String?): Builder {
            this.feedUrl = feedUrl?.toFeedUrl()
            return this
        }

        @Synchronized
        fun build(): CreateTribe? =
            if (!hasRequiredFields) {
                null
            } else {
                CreateTribe(
                    name = name!!,
                    description = description!!,
                    isTribe = isTribe,
                    pricePerMessage = pricePerMessage,
                    priceToJoin = priceToJoin,
                    escrowAmount = escrowAmount,
                    escrowMillis = escrowMillis,
                    img = img,
                    tags = tags.filter {
                        it.isSelected
                    }.map {
                        it.name
                    }.toTypedArray(),
                    unlisted = unlisted,
                    private = private,
                    appUrl = appUrl,
                    feedUrl = feedUrl
                )
            }
    }

    fun toPostGroupDto(imgUrl: String? = null): PostGroupDto {
        return PostGroupDto(
            name = name,
            description = description,
            is_tribe = isTribe,
            price_per_message = pricePerMessage,
            price_to_join = priceToJoin,
            escrow_amount = escrowAmount,
            escrow_millis = escrowMillis,
            img = imgUrl,
            tags = tags,
            unlisted = unlisted,
            private = private,
            app_url = appUrl?.value,
            feed_url = feedUrl?.value
        )
    }
}