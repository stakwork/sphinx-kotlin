package io.matthewnelson.concept_html_preview.model

data class HtmlPreview(
    val title: HtmlPreviewTitle?,
    val domainHost: HtmlPreviewDomainHost,
    val description: HtmlPreviewDescription?,
    val imageUrl: HtmlPreviewImageUrl?,
    val favIconUrl: HtmlPreviewFavIconUrl?
)
