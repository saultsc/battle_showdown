package saultsc.battle_showdown.client

/**
 * Page navigation of Cobblemon's battle challenge screen once preview pages exist.
 *
 * Cobblemon pages over a fixed enum of battle types, so instead of adding entries each previewable battle type
 * gets a second, virtual page: the screen stays on the same battle type and flips a "preview variant" flag.
 * Going forward the order is `Singles → Singles Preview → Doubles → Doubles Preview → …`.
 */
object ChallengePageNavigation {
    /**
     * The outcome of a page change request.
     *
     * @param cancelMove true when Cobblemon must stay on the current battle type.
     * @param previewVariant the variant flag after handling the request.
     * @param enterVariantAfterMove true when the variant must be turned on if the page Cobblemon lands on supports it.
     */
    data class PageChange(val cancelMove: Boolean, val previewVariant: Boolean, val enterVariantAfterMove: Boolean)

    /**
     * Decides what a click on the previous/next buttons does. Cobblemon asks for `(currentPage ± 1) % pageCount`,
     * so the direction is recovered from the requested value. With one or two pages both directions can ask for the
     * same value, in which case [backwardHint] (the side of the screen the mouse is on) breaks the tie.
     */
    fun onPageChange(
        currentPage: Int,
        requestedPage: Int,
        pageCount: Int,
        currentSupportsPreview: Boolean,
        previewVariant: Boolean,
        backwardHint: Boolean
    ): PageChange {
        if (pageCount <= 0) return PageChange(cancelMove = false, previewVariant = false, enterVariantAfterMove = false)

        val forwardRequest = (currentPage + 1) % pageCount
        val backwardRequest = (currentPage - 1) % pageCount
        val backward = if (forwardRequest == backwardRequest) backwardHint else requestedPage == backwardRequest

        return if (backward) {
            if (previewVariant) PageChange(cancelMove = true, previewVariant = false, enterVariantAfterMove = false)
            else PageChange(cancelMove = false, previewVariant = false, enterVariantAfterMove = true)
        } else {
            if (currentSupportsPreview && !previewVariant) PageChange(cancelMove = true, previewVariant = true, enterVariantAfterMove = false)
            else PageChange(cancelMove = false, previewVariant = false, enterVariantAfterMove = false)
        }
    }
}
