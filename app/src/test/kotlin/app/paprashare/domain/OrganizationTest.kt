package app.paprashare.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OrganizationTest {

    @Test
    fun parsesIdAndName_forEachOrganization() {
        val json = """{"organizations":[{"id":"org-a","name":"Maison"},{"id":"org-b","name":"Boulot"}]}"""
        assertEquals(
            listOf(Organization("org-a", "Maison"), Organization("org-b", "Boulot")),
            parseOrganizations(json),
        )
    }

    @Test
    fun ignoresUnknownFields_andKeyOrder() {
        val json = """{"organizations":[{"name":"Boulot","id":"org-b","plan":"premium","membersCount":2}]}"""
        assertEquals(listOf(Organization("org-b", "Boulot")), parseOrganizations(json))
    }

    @Test
    fun emptyList_returnsEmptyList() {
        assertEquals(emptyList<Organization>(), parseOrganizations("""{"organizations":[]}"""))
    }

    @Test
    fun invalidInput_returnsEmptyList_withoutThrowing() {
        assertEquals(emptyList<Organization>(), parseOrganizations("pas du json"))
        assertEquals(emptyList<Organization>(), parseOrganizations(""))
        assertEquals(emptyList<Organization>(), parseOrganizations("""{"foo":1}"""))
    }

    @Test
    fun serializeThenParse_roundTrips() {
        val orgs = listOf(Organization("a", "X"), Organization("b", "Y"))
        assertEquals(orgs, parseOrganizations(serializeOrganizations(orgs)))
    }
}
