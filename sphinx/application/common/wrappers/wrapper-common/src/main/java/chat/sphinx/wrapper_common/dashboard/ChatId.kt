package chat.sphinx.wrapper_common.dashboard

@JvmInline
value class ChatId(override val value: Long): DashboardItemId {

    companion object {
        const val NULL_CHAT_ID = Int.MAX_VALUE
    }

    init {
        require(this.value >= 0L) {
            "ChatId must be greater than or equal 0"
        }
    }

    override val dashboardItemType: DashboardItemType
        get() = DashboardItemType.Chat
}
