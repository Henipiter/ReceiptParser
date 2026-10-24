package com.henipiter.receipt.save

import com.henipiter.receipt.base.StoreBase
import com.henipiter.receipt.exception.StoreNotFoundException
import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import com.henipiter.receipt.fileanalyzer.parser.receipt.ReceiptParserFactory
import com.henipiter.receipt.fileanalyzer.parser.util.Stores
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ParseReceiptTest {

    private val bytes = byteArrayOf(1, 2, 3)

    private fun storeInDb(id: String, defaultCategoryId: String): StoreBase =
        mockk<StoreBase>(relaxed = true).also {
            every { it.id } returns id
            every { it.defaultCategoryId } returns defaultCategoryId
        }

    private fun parsedData(nip: String = NIP): ReceiptData {
        val store = mockk<StoreBase>(relaxed = true).also { every { it.nip } returns nip }
        return mockk<ReceiptData>(relaxed = true).also { every { it.store } returns store }
    }

    private class Setup(
        val factory: ReceiptParserFactory,
        val sut: ParseReceipt,
        val lookedUpNips: MutableList<String>,
    )

    private fun setup(
        storeLookup: StoreBase?,
        parsed: ReceiptData = parsedData(),
        store: Stores = Stores.LIDL,
    ): Setup {
        val factory = mockk<ReceiptParserFactory>(relaxed = true)
        val parser = factory.getParser(store)
        coEvery { parser.parse(any(), any(), any()) } returns parsed

        val nips = mutableListOf<String>()
        val sut = ParseReceipt(
            factory = factory,
            getStoreByNip = { nip -> nips += nip; storeLookup },
        )
        return Setup(factory, sut, nips)
    }

    @Test
    fun `store exists in db - uses its id and defaultCategoryId and returns parsed data`() = runTest {
        val parsed = parsedData()
        val s = setup(storeLookup = storeInDb("store-1", "db-category"), parsed = parsed)

        val result = s.sut.parse(Stores.LIDL, bytes, NIP, categoryId = "param-category")

        assertSame(parsed, result)
        val parser = s.factory.getParser(Stores.LIDL)
        coVerify(exactly = 1) { parser.parse(any(), "store-1", "db-category") }
    }

    @Test
    fun `store exists in db and categoryId param is null - still works`() = runTest {
        val parsed = parsedData()
        val s = setup(storeLookup = storeInDb("store-1", "db-category"), parsed = parsed)

        val result = s.sut.parse(Stores.LIDL, bytes, NIP, categoryId = null)

        assertSame(parsed, result)
        val parser = s.factory.getParser(Stores.LIDL)
        coVerify { parser.parse(any(), "store-1", "db-category") }
    }

    @Test
    fun `store exists in db with blank defaultCategoryId - db value wins and no exception`() = runTest {
        // elvis (?:) reaguje tylko na null, więc pusty string z bazy nie jest zastępowany parametrem
        val parsed = parsedData()
        val s = setup(storeLookup = storeInDb("store-1", ""), parsed = parsed)

        val result = s.sut.parse(Stores.LIDL, bytes, NIP, categoryId = "param-category")

        assertSame(parsed, result)
        val parser = s.factory.getParser(Stores.LIDL)
        coVerify { parser.parse(any(), "store-1", "") }
    }

    @Test
    fun `store missing in db but categoryId given - parses with empty storeId and given category`() = runTest {
        val parsed = parsedData()
        val s = setup(storeLookup = null, parsed = parsed)

        val result = s.sut.parse(Stores.LIDL, bytes, NIP, categoryId = "param-category")

        assertSame(parsed, result)
        val parser = s.factory.getParser(Stores.LIDL)
        coVerify(exactly = 1) { parser.parse(any(), "", "param-category") }
    }

    @Test
    fun `store missing in db and categoryId null - throws StoreNotFoundException after parsing`() = runTest {
        val s = setup(storeLookup = null)

        assertFailsWith<StoreNotFoundException> {
            s.sut.parse(Stores.LIDL, bytes, NIP, categoryId = null)
        }

        val parser = s.factory.getParser(Stores.LIDL)
        coVerify(exactly = 1) { parser.parse(any(), "", "") }
    }

    @Test
    fun `store missing in db and categoryId empty - throws StoreNotFoundException`() = runTest {
        val s = setup(storeLookup = null)

        assertFailsWith<StoreNotFoundException> {
            s.sut.parse(Stores.LIDL, bytes, NIP, categoryId = "")
        }
    }

    @Test
    fun `looks up store by the given nip exactly once`() = runTest {
        val s = setup(storeLookup = storeInDb("store-1", "db-category"))

        s.sut.parse(Stores.LIDL, bytes, NIP, categoryId = null)

        kotlin.test.assertEquals(listOf(NIP), s.lookedUpNips)
    }

    @Test
    fun `passes store type and bytes to factory`() = runTest {
        val s = setup(storeLookup = storeInDb("store-1", "db-category"), store = Stores.BIEDRONKA)

        s.sut.parse(Stores.BIEDRONKA, bytes, NIP, categoryId = null)

        coVerify(exactly = 1) { s.factory.getContent(Stores.BIEDRONKA, bytes) }
        coVerify(exactly = 1) { s.factory.getParser(Stores.BIEDRONKA) }
    }

    @Test
    fun `works for every Stores value`() = runTest {
        Stores.values().forEach { store ->
            val parsed = parsedData()
            val s = setup(storeLookup = storeInDb("store-1", "db-category"), parsed = parsed, store = store)

            val result = s.sut.parse(store, bytes, NIP, categoryId = null)

            assertSame(parsed, result, "Failed for $store")
        }
    }

    @Test
    fun `factory getContent fails - exception propagates and parser is not used`() = runTest {
        val factory = mockk<ReceiptParserFactory>(relaxed = true)
        coEvery { factory.getContent(any(), any()) } throws IllegalStateException("bad file")
        val sut = ParseReceipt(factory) { storeInDb("store-1", "db-category") }

        assertFailsWith<IllegalStateException> {
            sut.parse(Stores.LIDL, bytes, NIP, categoryId = null)
        }

        coVerify(exactly = 0) { factory.getParser(any()) }
    }

    @Test
    fun `parser fails - exception propagates`() = runTest {
        val factory = mockk<ReceiptParserFactory>(relaxed = true)
        val parser = factory.getParser(Stores.LIDL)
        coEvery { parser.parse(any(), any(), any()) } throws IllegalArgumentException("cannot parse")
        val sut = ParseReceipt(factory) { storeInDb("store-1", "db-category") }

        assertFailsWith<IllegalArgumentException> {
            sut.parse(Stores.LIDL, bytes, NIP, categoryId = null)
        }
    }

    @Test
    fun `getStoreByNip fails - exception propagates and parser is not used`() = runTest {
        val factory = mockk<ReceiptParserFactory>(relaxed = true)
        val sut = ParseReceipt(factory) { throw RuntimeException("db down") }

        assertFailsWith<RuntimeException> {
            sut.parse(Stores.LIDL, bytes, NIP, categoryId = null)
        }

        coVerify(exactly = 0) { factory.getParser(any()) }
    }

    private companion object {
        const val NIP = "7811897358"
    }
}