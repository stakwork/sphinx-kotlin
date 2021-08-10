package chat.sphinx.concept_link_preview.model

data class HtmlPreviewData(
    val title: HtmlPreviewTitle?,
    val domainHost: HtmlPreviewDomainHost,
    val description: PreviewDescription?,
    val imageUrl: PreviewImageUrl?,
    val favIconUrl: HtmlPreviewFavIconUrl?,
)
