package chat.sphinx.feature_repository.adapters.common

import chat.sphinx.wrapper_common.*
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.Sat
import com.squareup.sqldelight.ColumnAdapter

class PhotoUrlAdapter private constructor(): ColumnAdapter<PhotoUrl, String> {

    companion object {
        @Volatile
        private var instance: PhotoUrlAdapter? = null
        fun getInstance(): PhotoUrlAdapter =
            instance ?: synchronized(this) {
                instance ?: PhotoUrlAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): PhotoUrl {
        return PhotoUrl(databaseValue)
    }

    override fun encode(value: PhotoUrl): String {
        return value.value
    }
}

class DateTimeAdapter private constructor(): ColumnAdapter<DateTime, String> {

    companion object {
        @Volatile
        private var instance: DateTimeAdapter? = null
        fun getInstance(): DateTimeAdapter =
            instance ?: synchronized(this) {
                instance ?: DateTimeAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): DateTime {
        return databaseValue.toDateTime()
    }

    override fun encode(value: DateTime): String {
        return value.toString()
    }
}

class SatAdapter private constructor(): ColumnAdapter<Sat, Long> {

    companion object {
        @Volatile
        private var instance: SatAdapter? = null
        fun getInstance(): SatAdapter =
            instance ?: synchronized(this) {
                instance ?: SatAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): Sat {
        return Sat(databaseValue)
    }

    override fun encode(value: Sat): Long {
        return value.value
    }
}

class LightningNodePubKeyAdapter private constructor(): ColumnAdapter<LightningNodePubKey, String> {

    companion object {
        @Volatile
        private var instance: LightningNodePubKeyAdapter? = null
        fun getInstance(): LightningNodePubKeyAdapter =
            instance ?: synchronized(this) {
                instance ?: LightningNodePubKeyAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: String): LightningNodePubKey {
        return LightningNodePubKey(databaseValue)
    }

    override fun encode(value: LightningNodePubKey): String {
        return value.value
    }
}

class SeenAdapter private constructor(): ColumnAdapter<Seen, Long> {

    companion object {
        @Volatile
        private var instance: SeenAdapter? = null
        fun getInstance(): SeenAdapter =
            instance ?: synchronized(this) {
                instance ?: SeenAdapter()
                    .also { instance = it }
            }
    }

    override fun decode(databaseValue: Long): Seen {
        return databaseValue.toInt().toSeen()
    }

    override fun encode(value: Seen): Long {
        return value.value.toLong()
    }
}
