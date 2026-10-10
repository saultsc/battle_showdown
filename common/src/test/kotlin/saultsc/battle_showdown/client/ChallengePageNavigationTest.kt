package saultsc.battle_showdown.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChallengePageNavigationTest {
    /** Mirrors the challenge screen: Cobblemon's page index plus our preview variant flag. */
    private class Screen(private val supportsPreview: List<Boolean>) {
        var page = 0
        var variant = false

        val label: String
            get() = "$page" + if (variant) "p" else ""

        fun next() = click((page + 1) % supportsPreview.size, backwardHint = false)

        fun previous() = click((page - 1) % supportsPreview.size, backwardHint = true)

        private fun click(requested: Int, backwardHint: Boolean) {
            val change = ChallengePageNavigation.onPageChange(page, requested, supportsPreview.size, supportsPreview[page], variant, backwardHint)
            variant = change.previewVariant
            if (change.cancelMove) return

            // Cobblemon's own setCurrentPage
            page = if (requested > 0 && requested < supportsPreview.size) requested else if (requested < 0) supportsPreview.size - 1 else 0
            if (change.enterVariantAfterMove) variant = supportsPreview[page]
        }
    }

    private fun Screen.walk(steps: Int, move: Screen.() -> Unit): List<String> = List(steps) { move(); label }

    // Singles, Doubles, Triples, Multi, Royal
    private val defaultPages = listOf(true, true, false, false, false)

    @Test
    fun `going forward visits each preview page right after its battle type`() {
        val screen = Screen(defaultPages)
        assertEquals(listOf("0p", "1", "1p", "2", "3", "4", "0"), screen.walk(7) { next() })
    }

    @Test
    fun `going backward visits the same pages in reverse`() {
        val screen = Screen(defaultPages)
        assertEquals(listOf("4", "3", "2", "1p", "1", "0p", "0"), screen.walk(7) { previous() })
    }

    @Test
    fun `changing direction returns to the page just left`() {
        val screen = Screen(defaultPages)
        screen.next()
        screen.next()
        assertEquals("1", screen.label)
        screen.previous()
        assertEquals("0p", screen.label)
        screen.previous()
        assertEquals("0", screen.label)
    }

    @Test
    fun `two pages are still walked in both directions`() {
        val forward = Screen(listOf(true, true))
        assertEquals(listOf("0p", "1", "1p", "0"), forward.walk(4) { next() })

        val backward = Screen(listOf(true, true))
        assertEquals(listOf("1p", "1", "0p", "0"), backward.walk(4) { previous() })
    }

    @Test
    fun `a single page toggles its preview variant`() {
        val forward = Screen(listOf(true))
        assertEquals(listOf("0p", "0", "0p"), forward.walk(3) { next() })

        val backward = Screen(listOf(true))
        assertEquals(listOf("0p", "0", "0p"), backward.walk(3) { previous() })
    }

    @Test
    fun `pages without a preview format are not duplicated`() {
        val screen = Screen(listOf(false, false, false))
        assertEquals(listOf("1", "2", "0"), screen.walk(3) { next() })
    }
}
