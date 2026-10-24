package com.henipiter.receipt.fileanalyzer.parser.receipt


import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import com.henipiter.receipt.parsed.ProductParsed
import com.henipiter.receipt.util.PriceConverter.stringToPrice
import com.henipiter.receipt.util.PriceConverter.stringToQuantity
import com.henipiter.receipt.util.ResourceFileLoader
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.InputStreamReader

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LidlParserTest {

    private lateinit var receiptData: ReceiptData
    private lateinit var expectedProducts: List<ProductParsed>

    @Test
    fun `should parse whole receipt correctly`() {
        //given
        val filePath = "lidl/lidl_2026-04-24_33.15-zl.html"
        val content = ResourceFileLoader.readTextUtf8(filePath)
        expectedProducts = loadExpectedProducts()

        //when
        receiptData = LidlParser().parse(content, "", "")

        //then
        assertEquals("Lidl", receiptData.store.name)
        assertEquals("2026-04-24", receiptData.receipt.date)
        assertEquals("21:25", receiptData.receipt.time)
        assertEquals(3315, receiptData.receipt.pln)
        assertEquals(205, receiptData.receipt.ptu)

        assertAll(
            "Address",
            { assertEquals("60-307", receiptData.address.postalCode, "postalCode") },
            { assertEquals("Poznań", receiptData.address.city, "city") },
            { assertEquals("Grunwaldzka 104B", receiptData.address.streetName, "streetName") },
            { assertEquals(false, receiptData.address.favourite, "favourite") },
            { assertEquals(true, receiptData.address.autoSelect, "autoSelect") },
            { assertEquals("", receiptData.address.id, "id") },
            { assertEquals("", receiptData.address.storeId, "storeId") },
        )

        assertEquals(expectedProducts.size, receiptData.products.size, "Różna liczba produktów")

        expectedProducts.forEachIndexed { i, expected ->
            val actual = receiptData.products[i]
            assertAll(
                "Produkt[$i] '${expected.name}'",
                { assertEquals(expected.name, actual.name, "name") },
                { assertEquals(expected.ptuType, actual.ptuType, "taxType") },
                { assertEquals(expected.quantity, actual.quantity, "quantity") },
                { assertEquals(expected.unitPrice, actual.unitPrice, "priceUnit") },
                { assertEquals(expected.subtotalPrice, actual.subtotalPrice, "subtotalPrice") },
                { assertEquals(expected.discount, actual.discount, "discount") },
                { assertEquals(expected.finalPrice, actual.finalPrice, "finalPrice") },
            )
        }
    }

    private fun loadExpectedProducts(): List<ProductParsed> {
        val stream = Thread.currentThread().contextClassLoader
            .getResourceAsStream("lidl/lidl_2026-04-24_33.15-zl_expected.csv")
            ?: error("Nie znaleziono lidl_2026-04-24_33.15-zl_expected.csv w resources")

        return InputStreamReader(stream, Charsets.UTF_8).buffered().use { reader ->
            reader.readLines()
                .drop(1)
                .filter { it.isNotBlank() }
                .map { line ->
                    val cols = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                        .map { it.trim().removeSurrounding("\"") }

                    ProductParsed().apply {
                        name = cols[0]
                        ptuType = cols[1]
                        unitPrice = stringToPrice(cols[2])
                        quantity = stringToQuantity(cols[3])
                        subtotalPrice = stringToPrice(cols[4])
                        discount = stringToPrice(cols[5])
                        finalPrice = stringToPrice(cols[6])
                    }
                }
        }
    }


    @Test
    fun `should parse whole receipt correctly2`() {
        //given
        val filePath = "lidl/lidl_2026-08-31_118.75-zl.html"
        val content = ResourceFileLoader.readTextUtf8(filePath)
        expectedProducts = loadExpectedProducts2()

        //when
        receiptData = LidlParser().parse(content, "", "")

        //then
        assertEquals("Lidl", receiptData.store.name)
        assertEquals("2026-08-31", receiptData.receipt.date)
        assertEquals("20:48", receiptData.receipt.time)
        assertEquals(11875, receiptData.receipt.pln)
        assertEquals(622, receiptData.receipt.ptu)

        assertAll(
            "Address",
            { assertEquals("60-307", receiptData.address.postalCode, "postalCode") },
            { assertEquals("Poznań", receiptData.address.city, "city") },
            { assertEquals("Grunwaldzka 104B", receiptData.address.streetName, "streetName") },
            { assertEquals(false, receiptData.address.favourite, "favourite") },
            { assertEquals(true, receiptData.address.autoSelect, "autoSelect") },
            { assertEquals("", receiptData.address.id, "id") },
            { assertEquals("", receiptData.address.storeId, "storeId") },
        )

        assertEquals(expectedProducts.size, receiptData.products.size, "Różna liczba produktów")

        expectedProducts.forEachIndexed { i, expected ->
            val actual = receiptData.products[i]
            assertAll(
                "Produkt[$i] '${expected.name}'",
                { assertEquals(expected.name, actual.name, "name") },
                { assertEquals(expected.ptuType, actual.ptuType, "taxType") },
                { assertEquals(expected.quantity, actual.quantity, "quantity") },
                { assertEquals(expected.unitPrice, actual.unitPrice, "priceUnit") },
                { assertEquals(expected.subtotalPrice, actual.subtotalPrice, "subtotalPrice") },
                { assertEquals(expected.discount, actual.discount, "discount") },
                { assertEquals(expected.finalPrice, actual.finalPrice, "finalPrice") },
            )
        }
    }

    private fun loadExpectedProducts2(): List<ProductParsed> {
        val stream = Thread.currentThread().contextClassLoader
            .getResourceAsStream("lidl/lidl_2026-08-31_118.75-zl_expected.csv")
            ?: error("Nie znaleziono lidl_2026-08-31_118.75-zl_expected.csv w resources")

        return InputStreamReader(stream, Charsets.UTF_8).buffered().use { reader ->
            reader.readLines()
                .drop(1)
                .filter { it.isNotBlank() }
                .map { line ->
                    val cols = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                        .map { it.trim().removeSurrounding("\"") }

                    ProductParsed().apply {
                        name = cols[0]
                        ptuType = cols[1]
                        unitPrice = stringToPrice(cols[2])
                        quantity = stringToQuantity(cols[3])
                        subtotalPrice = stringToPrice(cols[4])
                        discount = stringToPrice(cols[5])
                        finalPrice = stringToPrice(cols[6])
                    }
                }
        }
    }
}