package chat.sphinx.example.concept_connect_manager.model

data class OwnerInfo(
    val alias: String,
    val picture: String,
    val userState: ByteArray?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OwnerInfo

        if (alias != other.alias) return false
        if (picture != other.picture) return false
        if (userState != null) {
            if (other.userState == null) return false
            if (!userState.contentEquals(other.userState)) return false
        } else if (other.userState != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alias.hashCode()
        result = 31 * result + picture.hashCode()
        result = 31 * result + (userState?.contentHashCode() ?: 0)
        return result
    }
}