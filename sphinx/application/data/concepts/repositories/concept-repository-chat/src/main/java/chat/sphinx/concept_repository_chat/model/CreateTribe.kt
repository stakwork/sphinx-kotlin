package chat.sphinx.concept_repository_chat.model

import chat.sphinx.concept_network_query_chat.model.NewTribeDto
import chat.sphinx.concept_network_query_chat.model.PostGroupDto
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.example.wrapper_mqtt.NewCreateTribe
import chat.sphinx.wrapper_chat.AppUrl
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.toFeedUrl
import chat.sphinx.wrapper_chat.toAppUrl
import chat.sphinx.wrapper_common.feed.FeedType
import chat.sphinx.wrapper_common.feed.toFeedType
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import java.io.File

class CreateTribe private constructor(
    val name: String,
    val description: String,
    val isTribe: Boolean?,
    val pricePerMessage: Long?,
    val priceToJoin: Long?,
    val escrowAmount: Long?,
    val escrowMillis: Long?,
    val img: File?,
    val imgUrl: String?,
    val tags: Array<String>,
    val unlisted: Boolean?,
    val private: Boolean?,
    val appUrl: AppUrl?,
    val feedUrl: FeedUrl?,
    val feedType: FeedType?,
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
        private var imgUrl: String? = null
        private var unlisted: Boolean? = false
        private var private: Boolean? = true
        private var appUrl: AppUrl? = null
        private var feedUrl: FeedUrl? = null
        private var feedType: FeedType? = null

        @get:Synchronized
        val hasRequiredFields: Boolean
            get() {
                val requiredFieldsFilled = !name.isNullOrEmpty() && !description.isNullOrEmpty()

                if (feedUrl != null) {
                    return (feedType != null) && requiredFieldsFilled
                }

                return requiredFieldsFilled
            }

        @Synchronized
        fun setName(name: String?): Builder {
            this.name = name
            return this
        }

        @Synchronized
        fun setImageUrl(imgUrl: String?): Builder {
            this.imgUrl = imgUrl
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

        @Synchronized
        fun toggleTag(index: Int): Builder {
            this.tags[index].isSelected = !this.tags[index].isSelected
            return this
        }

        fun selectedTags(): Array<Tag> {
            return tags.filter {
                it.isSelected
            }.toTypedArray()
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
        fun setFeedType(feedType: Int?): Builder {
            this.feedType = feedType?.toFeedType()
            return this
        }

        @Synchronized
        fun load(tribeDto: TribeDto) {
            name = tribeDto.name
            imgUrl = tribeDto.img
            img = null
            description = tribeDto.description
            tags.forEach { tag ->
                tag.isSelected = tribeDto.tags.contains(tag.name)
            }
            priceToJoin = tribeDto.price_to_join
            pricePerMessage = tribeDto.price_per_message
            escrowAmount = tribeDto.escrow_amount
            escrowMillis = tribeDto.escrow_millis
            appUrl = tribeDto.app_url?.toAppUrl()
            feedUrl = tribeDto.feed_url?.toFeedUrl()
            feedType = tribeDto.feed_type?.toFeedType()
            unlisted = tribeDto.unlisted
        }

        // Needs to complete all the args
        @Synchronized
        fun newLoad(tribeDto: NewTribeDto) {
            name = tribeDto.name
            img = null
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
                    imgUrl = imgUrl,
                    tags = selectedTags().map {
                        it.name
                    }.toTypedArray(),
                    unlisted = unlisted,
                    private = private,
                    appUrl = appUrl,
                    feedUrl = feedUrl,
                    feedType = feedType
                )
            }
    }

    fun toPostGroupDto(imageUrl: String? = null): PostGroupDto {
        return PostGroupDto(
            name = name,
            description = description,
            is_tribe = isTribe,
            price_per_message = pricePerMessage,
            price_to_join = priceToJoin,
            escrow_amount = escrowAmount,
            escrow_millis = escrowMillis,
            img = imageUrl ?: imgUrl,
            tags = tags,
            unlisted = unlisted,
            private = private,
            app_url = appUrl?.value,
            feed_url = feedUrl?.value,
            feed_type = feedType?.value?.toLong()
        )
    }

    fun toNewCreateTribe(ownerAlias: String, image: String?): NewCreateTribe {
        return NewCreateTribe(
            pubkey = null,
            route_hint = null,
            name = this.name,
            description = this.description,
            tags = this.tags.toList(),
            img = image ?: this.imgUrl,
            price_per_message = this.pricePerMessage,
            price_to_join = this.priceToJoin,
            escrow_amount = this.escrowAmount,
            escrow_millis = this.escrowMillis,
            unlisted = this.unlisted,
            private = this.private,
            app_url = this.appUrl?.value,
            feed_url = this.feedUrl?.value,
            feed_type = this.feedType?.value,
            created = null,
            updated = null,
            member_count = null,
            last_active = null,
            owner_alias = ownerAlias
        )
    }
}