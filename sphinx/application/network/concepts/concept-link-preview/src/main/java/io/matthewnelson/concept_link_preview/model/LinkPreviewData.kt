package io.matthewnelson.concept_link_preview.model

data class LinkPreviewData(
    val title: HtmlPreviewTitle?,
    val domainHost: LinkPreviewDomainHost,
    val description: HtmlPreviewDescription?,
    val imageUrl: HtmlPreviewImageUrl?,
    val favIconUrl: HtmlPreviewFavIconUrl?
)
