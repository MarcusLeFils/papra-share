package app.paprashare.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OrganizationSelectionTest {

    @Test
    fun lastUsedId_takesPrecedence_whenPresent() {
        assertEquals(
            "org-b",
            defaultOrganizationId(listOf("org-a", "org-b", "org-c"), savedId = "org-a", lastUsedId = "org-b"),
        )
    }

    @Test
    fun savedId_isUsed_whenLastChoiceAbsentFromList() {
        assertEquals(
            "org-a",
            defaultOrganizationId(listOf("org-a", "org-b"), savedId = "org-a", lastUsedId = "zzz"),
        )
    }

    @Test
    fun savedId_isUsed_whenNoLastChoice() {
        assertEquals(
            "org-a",
            defaultOrganizationId(listOf("org-a", "org-b"), savedId = "org-a", lastUsedId = null),
        )
    }

    @Test
    fun firstOfList_whenNeitherSavedNorLastPresent() {
        assertEquals(
            "org-a",
            defaultOrganizationId(listOf("org-a", "org-b"), savedId = "zzz", lastUsedId = "yyy"),
        )
    }

    @Test
    fun null_whenEmptyList() {
        assertEquals(
            null,
            defaultOrganizationId(emptyList(), savedId = "org-a", lastUsedId = "org-b"),
        )
    }
}