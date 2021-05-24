package chat.sphinx.wrapper_contact

@Suppress("NOTHING_TO_INLINE")
inline fun String.toNotificationSound(): NotificationSound =
    when (this) {
        NotificationSound.TRI_TONE -> {
            NotificationSound.TriTone
        }
        NotificationSound.AURORA -> {
            NotificationSound.Aurora
        }
        NotificationSound.BAMBOO -> {
            NotificationSound.Bamboo
        }
        NotificationSound.BELL -> {
            NotificationSound.Bell
        }
        NotificationSound.BELLS -> {
            NotificationSound.Bells
        }
        NotificationSound.GLASS -> {
            NotificationSound.Glass
        }
        NotificationSound.HORN -> {
            NotificationSound.Horn
        }
        NotificationSound.NOTE -> {
            NotificationSound.Note
        }
        NotificationSound.POPCORN -> {
            NotificationSound.Popcorn
        }
        NotificationSound.SYNTH -> {
            NotificationSound.Synth
        }
        NotificationSound.TWEET -> {
            NotificationSound.Tweet
        }
        else -> {
            NotificationSound.Unknown(this)
        }
    }

/**
 * Comes off the wire as:
 *  - tri-tone.caf (TriTone)
 *  - aurora.caf (Aurora)
 *  - bamboo.caf (Bamboo)
 *  - bell.caf (Bell)
 *  - bells.caf (Bells)
 *  - glass.caf (Glass)
 *  - horn.caf (Horn)
 *  - note.caf (Note)
 *  - popcorn.caf (Popcorn)
 *  - synth.caf (Synth)
 *  - tweet.caf (Tweet)
 * */
@Suppress("SpellCheckingInspection")
sealed class NotificationSound {

    companion object {
        const val TRI_TONE = "tri-tone.caf"
        const val AURORA = "aurora.caf"
        const val BAMBOO = "bamboo.caf"
        const val BELL = "bell.caf"
        const val BELLS = "bells.caf"
        const val GLASS = "glass.caf"
        const val HORN = "horn.caf"
        const val NOTE = "note.caf"
        const val POPCORN = "popcorn.caf"
        const val SYNTH = "synth.caf"
        const val TWEET = "tweet.caf"
    }

    abstract val value: String

    object TriTone: NotificationSound() {
        override val value: String
            get() = TRI_TONE
    }

    object Aurora: NotificationSound() {
        override val value: String
            get() = AURORA
    }

    object Bamboo: NotificationSound() {
        override val value: String
            get() = BAMBOO
    }

    object Bell: NotificationSound() {
        override val value: String
            get() = BELL
    }

    object Bells: NotificationSound() {
        override val value: String
            get() = BELLS
    }

    object Glass: NotificationSound() {
        override val value: String
            get() = GLASS
    }

    object Horn: NotificationSound() {
        override val value: String
            get() = HORN
    }

    object Note: NotificationSound() {
        override val value: String
            get() = NOTE
    }

    object Popcorn: NotificationSound() {
        override val value: String
            get() = POPCORN
    }

    object Synth: NotificationSound() {
        override val value: String
            get() = SYNTH
    }

    object Tweet: NotificationSound() {
        override val value: String
            get() = TWEET
    }

    data class Unknown(override val value: String) : NotificationSound()
}
