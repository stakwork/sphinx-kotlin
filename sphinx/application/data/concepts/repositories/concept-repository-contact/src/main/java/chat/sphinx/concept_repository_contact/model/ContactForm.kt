package chat.sphinx.concept_repository_contact.model

import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import chat.sphinx.wrapper_common.lightning.toLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.toLightningRouteHint
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.toContactAlias

class ContactForm private constructor(
    val contactAlias: ContactAlias,
    val lightningNodePubKey: LightningNodePubKey,
    val lightningRouteHint: LightningRouteHint?,
){
    class Builder {
        private var contactAlias: ContactAlias? = null
        private var lightningNodePubKey: LightningNodePubKey? = null
        private var lightningRouteHint: String? = null

        @Synchronized
        fun clear() {
            contactAlias = null
            lightningNodePubKey = null
            lightningRouteHint = null
        }

        @get:Synchronized
        val isValid: Boolean
            get() = hasContactAlias && hasLightningNodePubKey

        @get:Synchronized
        val hasContactAlias: Boolean
            get() = contactAlias != null

        @get:Synchronized
        val hasLightningNodePubKey: Boolean
            get() = lightningNodePubKey != null


        val hasValidLightningRouteHint: Boolean
            get() = lightningRouteHint.isNullOrEmpty() || lightningRouteHint?.toLightningRouteHint() != null

        @Synchronized
        fun setContactAlias(alias: String?): Builder {
            contactAlias = alias?.toContactAlias()
            return this
        }

        @Synchronized
        fun setLightningNodePubKey(lightningNodePubKey: String?): Builder {
            this.lightningNodePubKey = lightningNodePubKey?.toLightningNodePubKey()
            return this
        }

        @Synchronized
        fun setLightningRouteHint(lightningRouteHint: String?): Builder {
            this.lightningRouteHint = lightningRouteHint
            return this
        }

        @Synchronized
        fun build(): ContactForm? {
            contactAlias?.let { contactAlias ->
                lightningNodePubKey?.let { lightningNodePubKey ->
                    return ContactForm(
                        contactAlias,
                        lightningNodePubKey,
                        lightningRouteHint?.toLightningRouteHint()
                    )
                }
            }

            return null
        }
    }
}