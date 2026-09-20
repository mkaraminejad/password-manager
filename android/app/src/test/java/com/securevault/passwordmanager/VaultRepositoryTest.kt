package com.securevault.passwordmanager

import com.securevault.passwordmanager.core.model.VaultEntry
import com.securevault.passwordmanager.data.local.VaultDao
import com.securevault.passwordmanager.data.local.VaultEntity
import com.securevault.passwordmanager.data.repository.VaultRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VaultRepositoryTest {

    private val dao = mockk<VaultDao>(relaxed = true)
    private lateinit var repository: VaultRepositoryImpl

    @Before
    fun setup() {
        repository = VaultRepositoryImpl(dao)
    }

    @Test
    fun testInsertAndRetrieveEntries() = runBlocking {
        val entry = VaultEntry(
            id = "test-1",
            title = "GitHub",
            username = "octocat",
            password = "SecureToken123!"
        )

        every { dao.getAllEntries() } returns flowOf(listOf(VaultEntity.fromDomain(entry)))

        val list = repository.getAllEntries().first()
        assertEquals(1, list.size)
        assertEquals("GitHub", list[0].title)
        assertEquals("octocat", list[0].username)
    }

    @Test
    fun testDuplicateEntryCallsInsert() = runBlocking {
        val original = VaultEntry(
            id = "test-orig",
            title = "Personal Email",
            username = "user@mail.com",
            password = "SecretPassword123"
        )

        coEvery { dao.getEntryById("test-orig") } returns VaultEntity.fromDomain(original)

        repository.duplicateEntry("test-orig")

        coVerify { dao.insertEntry(match { it.title == "Personal Email (Copy)" && it.id != "test-orig" }) }
    }
}
