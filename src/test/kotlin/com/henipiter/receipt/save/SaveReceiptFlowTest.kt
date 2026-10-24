package com.henipiter.receipt.save

import com.henipiter.receipt.base.AddressBase
import com.henipiter.receipt.base.ProductBase
import com.henipiter.receipt.base.ReceiptBase
import com.henipiter.receipt.base.StoreBase
import com.henipiter.receipt.dto.ReceiptExtras
import com.henipiter.receipt.exception.InvalidReceiptException
import com.henipiter.receipt.exception.ReceiptAlreadyExistsException
import com.henipiter.receipt.exception.StoreNotFoundException
import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SaveReceiptFlowTest {

    // ---------- stateful mocks ----------

    private fun fakeStore(
        id: String = "",
        nip: String = NIP,
        addressId: String = "",
        defaultCategoryId: String = "",
    ): StoreBase = mockk<StoreBase>(relaxed = true).also { store ->
        var _id = id
        var _addressId = addressId
        var _defaultCategoryId = defaultCategoryId
        every { store.id } answers { _id }
        every { store.id = any() } answers { _id = firstArg() }
        every { store.nip } returns nip
        every { store.addressId } answers { _addressId }
        every { store.addressId = any() } answers { _addressId = firstArg() }
        every { store.defaultCategoryId } answers { _defaultCategoryId }
        every { store.defaultCategoryId = any() } answers { _defaultCategoryId = firstArg() }
    }

    private fun fakeAddress(
        id: String = "",
        storeId: String = "",
        postalCode: String = POSTAL_CODE,
    ): AddressBase = mockk<AddressBase>(relaxed = true).also { address ->
        var _id = id
        var _storeId = storeId
        every { address.id } answers { _id }
        every { address.id = any() } answers { _id = firstArg() }
        every { address.storeId } answers { _storeId }
        every { address.storeId = any() } answers { _storeId = firstArg() }
        every { address.city } returns "Poznań"
        every { address.postalCode } returns postalCode
        every { address.streetName } returns "Półwiejska"
    }

    private fun fakeReceipt(online: Boolean = false, pln: Int = 1999): ReceiptBase =
        mockk<ReceiptBase>(relaxed = true).also { receipt ->
            var _id = ""
            var _storeId = ""
            var _addressId = ""
            var _payerId = ""
            var _beneficiaryId = ""
            every { receipt.id } answers { _id }
            every { receipt.id = any() } answers { _id = firstArg() }
            every { receipt.storeId } answers { _storeId }
            every { receipt.storeId = any() } answers { _storeId = firstArg() }
            every { receipt.addressId } answers { _addressId }
            every { receipt.addressId = any() } answers { _addressId = firstArg() }
            every { receipt.payerId } answers { _payerId }
            every { receipt.payerId = any() } answers { _payerId = firstArg() }
            every { receipt.beneficiaryId } answers { _beneficiaryId }
            every { receipt.beneficiaryId = any() } answers { _beneficiaryId = firstArg() }
            every { receipt.online } returns online
            every { receipt.date } returns "2026-10-24"
            every { receipt.time } returns "12:30"
            every { receipt.pln } returns pln
        }

    private fun fakeProduct(): ProductBase = mockk<ProductBase>(relaxed = true).also { product ->
        var _receiptId = ""
        var _categoryId = ""
        var _beneficiaryId = ""
        every { product.receiptId } answers { _receiptId }
        every { product.receiptId = any() } answers { _receiptId = firstArg() }
        every { product.categoryId } answers { _categoryId }
        every { product.categoryId = any() } answers { _categoryId = firstArg() }
        every { product.beneficiaryId } answers { _beneficiaryId }
        every { product.beneficiaryId = any() } answers { _beneficiaryId = firstArg() }
        every { product.name } returns "Mleko"
        every { product.quantity } returns 1
        every { product.unitPrice } returns 299
        every { product.subtotalPrice } returns 299
        every { product.finalPrice } returns 299
    }

    private fun extras(categoryId: String?): ReceiptExtras =
        mockk<ReceiptExtras>(relaxed = true).also {
            every { it.categoryId } returns categoryId
            every { it.payerId } returns PAYER_ID
            every { it.beneficiaryId } returns BENEFICIARY_ID
        }

    // ---------- "baza danych" i flow ----------

    private class Harness {
        var storeInDb: StoreBase? = null
        var receiptUnique = true
        var addressById: AddressBase? = null
        var addressByPostalCode: AddressBase? = null

        val insertedStores = mutableListOf<StoreBase>()
        val insertedReceipts = mutableListOf<ReceiptBase>()
        val insertedAddresses = mutableListOf<AddressBase>()
        val addressIdsAtInsert = mutableListOf<String>()
        val insertedProducts = mutableListOf<ProductBase>()
        var postalCodeLookups = 0
        var onInsertStore: (StoreBase) -> StoreBase = { it }

        val flow = SaveReceiptFlow(
            getStoreByNip = { storeInDb },
            checkIsReceiptUnique = { receiptUnique },
            insertSingleProduct = { insertedProducts += it },
            insertStore = { store ->
                insertedStores += store
                storeInDb = onInsertStore(store)
                storeInDb!!.id
            },
            insertReceipt = { insertedReceipts += it; NEW_RECEIPT_ID },
            insertAddress = { a ->
                insertedAddresses += a
                addressIdsAtInsert += a.id
                a.id.ifEmpty { NEW_ADDRESS_ID }
            },
            getAddressById = { addressById },
            getAddressByPostalCode = { _, _ -> postalCodeLookups++; addressByPostalCode },
        )
    }

    private fun harness(block: Harness.() -> Unit = {}) = Harness().apply(block)

    private fun receiptData(online: Boolean = false, pln: Int = 1999) = ReceiptData(
        address = fakeAddress(),
        store = fakeStore(),
        receipt = fakeReceipt(online, pln),
        products = listOf(fakeProduct(), fakeProduct()),
    )

    // ---------- testy ----------

    @Test
    fun `store does not exist and categoryId is missing - throws StoreNotFoundException`(): Unit = runTest {
        val h = harness { storeInDb = null }
        val data = receiptData()

        assertFailsWith<StoreNotFoundException> { h.flow.saveData(data, extras(categoryId = null)) }
        assertFailsWith<StoreNotFoundException> { h.flow.saveData(data, extras(categoryId = "")) }

        assertTrue(h.insertedStores.isEmpty())
        assertTrue(h.insertedAddresses.isEmpty())
        assertTrue(h.insertedReceipts.isEmpty())
        assertTrue(h.insertedProducts.isEmpty())
    }

    @Test
    fun `store does not exist but categoryId is given - creates store, address, receipt and products`(): Unit =
        runTest {
            val h = harness {
                storeInDb = null
                addressByPostalCode = null
                onInsertStore = { s ->
                    fakeStore(id = NEW_STORE_ID, addressId = s.addressId, defaultCategoryId = s.defaultCategoryId)
                }
            }
            val data = receiptData()

            h.flow.saveData(data, extras(categoryId = CATEGORY_ID))

            assertEquals(1, h.insertedStores.size)
            assertEquals(CATEGORY_ID, h.insertedStores.single().defaultCategoryId)
            assertTrue(h.insertedStores.single().addressId.isNotEmpty())

            assertEquals(NEW_STORE_ID, data.receipt.storeId)
            assertEquals(NEW_STORE_ID, data.address.storeId)

            assertEquals(1, h.insertedAddresses.size)
            assertEquals(1, h.insertedReceipts.size)
            assertEquals(PAYER_ID, data.receipt.payerId)
            assertEquals(BENEFICIARY_ID, data.receipt.beneficiaryId)

            assertEquals(2, h.insertedProducts.size)
            h.insertedProducts.forEach {
                assertEquals(CATEGORY_ID, it.categoryId)
                assertEquals(NEW_RECEIPT_ID, it.receiptId)
                assertEquals(BENEFICIARY_ID, it.beneficiaryId)
            }
            assertTrue(data.store.addressId.isNotEmpty())
            assertEquals(data.store.addressId, h.addressIdsAtInsert.single())
            assertEquals(data.store.addressId, data.address.id)
            assertEquals(data.store.addressId, data.receipt.addressId)
            assertEquals(0, h.postalCodeLookups)
        }

    @Test
    fun `store exists - does not insert store and uses its id and default category`(): Unit = runTest {
        val h = harness {
            storeInDb = fakeStore(id = EXISTING_STORE_ID, defaultCategoryId = STORE_DEFAULT_CATEGORY)
            addressByPostalCode = null
        }
        val data = receiptData()

        // categoryId z extras jest ignorowane, bo sklep już istnieje
        h.flow.saveData(data, extras(categoryId = CATEGORY_ID))

        assertTrue(h.insertedStores.isEmpty())
        assertEquals(EXISTING_STORE_ID, data.receipt.storeId)
        assertEquals(EXISTING_STORE_ID, data.address.storeId)
        assertEquals(EXISTING_STORE_ID, data.store.id)
        assertEquals(1, h.insertedReceipts.size)
        assertEquals(2, h.insertedProducts.size)
        h.insertedProducts.forEach { assertEquals(STORE_DEFAULT_CATEGORY, it.categoryId) }
    }

    @Test
    fun `receipt is duplicated - throws ReceiptAlreadyExistsException and saves nothing`(): Unit = runTest {
        val h = harness {
            storeInDb = fakeStore(id = EXISTING_STORE_ID)
            receiptUnique = false
        }
        val data = receiptData()

        assertFailsWith<ReceiptAlreadyExistsException> {
            h.flow.saveData(data, extras(categoryId = CATEGORY_ID))
        }

        assertTrue(h.insertedStores.isEmpty())
        assertTrue(h.insertedAddresses.isEmpty())
        assertTrue(h.insertedReceipts.isEmpty())
        assertTrue(h.insertedProducts.isEmpty())
    }

    @Test
    fun `address exists - reuses it and does not insert a new one`(): Unit = runTest {
        val existingAddress = fakeAddress(id = EXISTING_ADDRESS_ID, storeId = EXISTING_STORE_ID)
        val h = harness {
            storeInDb = fakeStore(id = EXISTING_STORE_ID)
            addressByPostalCode = existingAddress
        }
        val data = receiptData()

        h.flow.saveData(data, extras(categoryId = null))

        assertTrue(h.insertedAddresses.isEmpty())
        assertEquals(EXISTING_ADDRESS_ID, data.address.id)
        assertEquals(EXISTING_ADDRESS_ID, data.receipt.addressId)
        assertEquals(1, h.insertedReceipts.size)
        assertEquals(2, h.insertedProducts.size)
    }

    @Test
    fun `address does not exist - inserts it and links receipt to the new address`(): Unit = runTest {
        val h = harness {
            storeInDb = fakeStore(id = EXISTING_STORE_ID)
            addressByPostalCode = null
        }
        val data = receiptData()

        h.flow.saveData(data, extras(categoryId = null))

        assertEquals(1, h.insertedAddresses.size)
        assertEquals(EXISTING_STORE_ID, h.insertedAddresses.single().storeId)
        assertEquals(NEW_ADDRESS_ID, data.address.id)
        assertEquals(NEW_ADDRESS_ID, data.receipt.addressId)
        assertEquals(NEW_RECEIPT_ID, data.receipt.id)
    }

    @Test
    fun `online receipt without existing address - inserts address with store addressId and skips postal code lookup`() =
        runTest {
            val h = harness {
                storeInDb = fakeStore(id = EXISTING_STORE_ID, addressId = ONLINE_ADDRESS_ID)
                addressById = null
            }
            val data = receiptData(online = true)

            h.flow.saveData(data, extras(categoryId = null))

            assertEquals(1, h.insertedAddresses.size)
            assertEquals(ONLINE_ADDRESS_ID, h.addressIdsAtInsert.single())
            assertEquals(EXISTING_STORE_ID, h.insertedAddresses.single().storeId)
            assertEquals(0, h.postalCodeLookups)
            assertEquals(ONLINE_ADDRESS_ID, data.receipt.addressId)
        }

    @Test
    fun `online receipt with existing address - reuses it and does not insert`() = runTest {
        val existing = fakeAddress(id = ONLINE_ADDRESS_ID, storeId = EXISTING_STORE_ID)
        val h = harness {
            storeInDb = fakeStore(id = EXISTING_STORE_ID, addressId = ONLINE_ADDRESS_ID)
            addressById = existing
        }
        val data = receiptData(online = true)

        h.flow.saveData(data, extras(categoryId = null))

        assertTrue(h.insertedAddresses.isEmpty())
        assertEquals(0, h.postalCodeLookups)
        assertEquals(ONLINE_ADDRESS_ID, data.receipt.addressId)
    }

    @Test
    fun `invalid receipt data - returns silently and saves nothing`() = runTest {
        val h = harness { storeInDb = null }
        val data = receiptData(pln = 0)
        assertFailsWith<InvalidReceiptException> { h.flow.saveData(data, extras(categoryId = null)) }

        assertTrue(h.insertedStores.isEmpty())
        assertTrue(h.insertedAddresses.isEmpty())
        assertTrue(h.insertedReceipts.isEmpty())
        assertTrue(h.insertedProducts.isEmpty())
    }

    private companion object {
        const val NIP = "7791011327"
        const val POSTAL_CODE = "60-001"
        const val CATEGORY_ID = "category-from-extras"
        const val STORE_DEFAULT_CATEGORY = "category-from-store"
        const val PAYER_ID = "payer-1"
        const val BENEFICIARY_ID = "beneficiary-1"
        const val NEW_STORE_ID = "new-store-id"
        const val EXISTING_STORE_ID = "existing-store-id"
        const val NEW_ADDRESS_ID = "new-address-id"
        const val EXISTING_ADDRESS_ID = "existing-address-id"
        const val NEW_RECEIPT_ID = "new-receipt-id"
        const val ONLINE_ADDRESS_ID = "online-address-id"
    }
}