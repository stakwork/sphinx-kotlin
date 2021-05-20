package chat.sphinx.wrapper_message


/**
 * Messages are consider "paid" if they have a type equalling `ATTACHMENT`,
 * and if the price that can be extracted from the mediaToken is greater than 0.
 */
inline val Message.isPaidMessage: Boolean
    get() {
         // TODO: Implement logic at the repository level for extracting a price from the media token.
//        return isAttachment && messageMedia.priceFromToken > 0
        return false
    }

inline val Message.isAttachment: Boolean
    get() {
        return type.isAttachment()
    }
