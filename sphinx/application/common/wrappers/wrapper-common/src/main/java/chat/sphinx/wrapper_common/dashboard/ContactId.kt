package chat.sphinx.wrapper_common.dashboard

@Suppress("NOTHING_TO_INLINE")
inline fun Long.toContactId(): ContactId? =
    try {
        ContactId(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ContactId(override val value: Long): DashboardItemId {
    override val dashboardItemType: DashboardItemType
        get() = DashboardItemType.Contact
}
