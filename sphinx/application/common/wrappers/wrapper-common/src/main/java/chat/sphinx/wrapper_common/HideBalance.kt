package chat.sphinx.wrapper_common

sealed class HideBalance {

    companion object {
        const val ENABLED = 1
        const val DISABLED = 0

        const val HIDE_BALANCE_SHARED_PREFERENCES = "general_settings"
        const val HIDE_BALANCE_ENABLED_KEY = "hide-balance-enabled"
    }

    abstract val value: Int

    object True: HideBalance(){
        override val value: Int
            get() = ENABLED
    }

    object False: HideBalance(){
        override val value: Int
            get() = DISABLED
    }
}