package chat.sphinx.wrapper_common.dashboard

sealed class DashboardItemType(
    val typeString: String,
    val sortPriority: Int,
) {

    companion object {
        const val DASHBOARD_TYPE_CHAT_KEY = "cha"
        const val DASHBOARD_TYPE_CHAT_PRIORITY = 2

        const val DASHBOARD_TYPE_CONTACT_KEY = "con"
        const val DASHBOARD_TYPE_CONTACT_PRIORITY = DASHBOARD_TYPE_CHAT_PRIORITY

        const val DASHBOARD_TYPE_INVITE_KEY = "inv"
        const val DASHBOARD_TYPE_INVITE_PRIORITY = 1
    }

    object Chat: DashboardItemType(
        DASHBOARD_TYPE_CHAT_KEY,
        DASHBOARD_TYPE_CHAT_PRIORITY,
    )

    object Contact: DashboardItemType(
        DASHBOARD_TYPE_CONTACT_KEY,
        DASHBOARD_TYPE_CONTACT_PRIORITY,
    )

    object Invite: DashboardItemType(
        DASHBOARD_TYPE_INVITE_KEY,
        DASHBOARD_TYPE_INVITE_PRIORITY,
    )
}

@Suppress("NOTHING_TO_INLINE")
inline fun DashboardItemId.toDashboardIdString(): String =
    dashboardItemType.sortPriority.toString()   +
    DashboardItemId.DELIMITER                   +
    dashboardItemType.typeString                +
    DashboardItemId.DELIMITER                   +
    value

@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class, NumberFormatException::class)
inline fun String.toDashboardItemId(): DashboardItemId {
    split(DashboardItemId.DELIMITER).let { splits ->
        if (splits.size != 3) {
            throw IllegalArgumentException(
                "Invalid delimiter to convert String to a DashboardItemId"
            )
        }

        return when (splits[1]) {
            DashboardItemType.Chat.typeString -> {
                ChatId(splits[2].toLong())
            }
            DashboardItemType.Contact.typeString -> {
                ContactId(splits[2].toLong())
            }
            DashboardItemType.Invite.typeString -> {
                InviteId(splits[2].toLong())
            }
            else -> {
                throw IllegalArgumentException(
                    "String value did not match any of the available DashboardItemTypes"
                )
            }
        }
    }
}

/**
 * This is for the Dashboard Table which is a culmination
 * of Chats, Contacts w/o a chat yet, and Invites. The
 * [ChatId], [ContactId], and [InviteId] gets stored as the
 * primary key in a specific string format to promote sorting
 * as well as ensuring values will always be unique.
 *
 * The id field for the DB Table is type TEXT that is stored
 * as <sort priority>|<type>|<id value>
 * */
sealed interface DashboardItemId {

    companion object {
        const val DELIMITER = '|'
    }

    val dashboardItemType: DashboardItemType
    val value: Long
}
