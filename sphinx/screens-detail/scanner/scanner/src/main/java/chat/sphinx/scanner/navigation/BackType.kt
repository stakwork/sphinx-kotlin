package chat.sphinx.scanner.navigation

internal sealed class BackType {
    object PopBackStack: BackType()
    object CloseDetailScreen: BackType()
}
