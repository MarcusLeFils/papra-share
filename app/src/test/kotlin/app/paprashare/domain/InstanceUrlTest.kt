package app.paprashare.domain

import org.junit.Test
import org.junit.Assert

class InstanceUrlTest {

    @Test
    fun trailingSlash_isStripped() {
        Assert.assertEquals("https://api.papra.app", normalizeInstanceUrl("https://api.papra.app/"))
    }

    @Test
    fun surroundingWhitespace_isTrimmed() {
        Assert.assertEquals("https://api.papra.app", normalizeInstanceUrl("  https://api.papra.app  "))
    }

    @Test
    fun noTrailingSlash_isLeftUntouched() {
        Assert.assertEquals("https://api.papra.app", normalizeInstanceUrl("https://api.papra.app"))
    }

    @Test
    fun whitespaceAndSlash_areBothHandled() {
        Assert.assertEquals("https://api.papra.app", normalizeInstanceUrl(" https://api.papra.app/ "))
    }

    @Test
    fun onlyRootSlash() {
        Assert.assertEquals("https://example.org", normalizeInstanceUrl("https://example.org/"))
    }
}