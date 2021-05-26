package chat.sphinx.qr_code.navigation

internal sealed class BackType {
    object PopBackStack: BackType()
    object CloseDetailScreen: BackType()
}
