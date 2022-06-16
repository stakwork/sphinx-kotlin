package chat.sphinx.feature_coredb.adapters.media

import chat.sphinx.wrapper_message_media.*
import com.squareup.sqldelight.ColumnAdapter

internal class MediaKeyAdapter: ColumnAdapter<MediaKey, String> {
    override fun decode(databaseValue: String): MediaKey {
        return MediaKey(databaseValue)
    }

    override fun encode(value: MediaKey): String {
        return value.value
    }
}

internal class MediaKeyDecryptedAdapter: ColumnAdapter<MediaKeyDecrypted, String> {
    override fun decode(databaseValue: String): MediaKeyDecrypted {
        return MediaKeyDecrypted(databaseValue)
    }

    override fun encode(value: MediaKeyDecrypted): String {
        return value.value
    }
}

internal class MediaTypeAdapter: ColumnAdapter<MediaType, String> {
    override fun decode(databaseValue: String): MediaType {
        return databaseValue.toMediaType()
    }

    override fun encode(value: MediaType): String {
        return value.value
    }
}

internal class MediaTokenAdapter: ColumnAdapter<MediaToken, String> {
    override fun decode(databaseValue: String): MediaToken {
        return MediaToken(databaseValue)
    }

    override fun encode(value: MediaToken): String {
        return value.value
    }
}

internal class FileNameAdapter: ColumnAdapter<FileName, String> {
    override fun decode(databaseValue: String): FileName {
        return FileName(databaseValue)
    }

    override fun encode(value: FileName): String {
        return value.value
    }
}
