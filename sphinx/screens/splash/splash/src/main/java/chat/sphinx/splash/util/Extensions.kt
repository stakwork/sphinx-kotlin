package chat.sphinx.splash.util

inline val String.isInviteCode: Boolean
    get() = matches("^[A-F0-9a-f]{40}\$".toRegex())
