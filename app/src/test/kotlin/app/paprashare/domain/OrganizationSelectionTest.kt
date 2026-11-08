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
    fun savedId_isPreserved_evenWhenAbsentFromList() {
        // La cible configurée (v1.x) ne doit pas être remplacée en silence par
        // la première organisation listée — même si elle n'apparaît pas dans la liste.
        assertEquals(
            "cfg",
            defaultOrganizationId(listOf("org-a", "org-b"), savedId = "cfg", lastUsedId = "zzz"),
        )
        assertEquals(
            "cfg",
            defaultOrganizationId(listOf("org-a", "org-b"), savedId = "cfg", lastUsedId = null),
        )
    }

    @Test
    fun firstOfList_whenNoConfiguredId() {
        assertEquals(
            "org-a",
            defaultOrganizationId(listOf("org-a", "org-b"), savedId = "", lastUsedId = null),
        )
    }

    @Test
    fun blankLastUsedId_isIgnored() {
        assertEquals(
            "org-a",
            defaultOrganizationId(listOf("org-a", "org-b"), savedId = "org-a", lastUsedId = ""),
        )
    }

    @Test
    fun null_whenNoConfiguredIdAndEmptyList() {
        assertEquals(
            null,
            defaultOrganizationId(emptyList(), savedId = "", lastUsedId = "org-b"),
        )
    }
}
