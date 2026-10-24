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
class BiedronkaParserTest {

    private lateinit var receiptData: ReceiptData
    private lateinit var expectedProducts: List<ProductParsed>

    @Test
    fun `should parse all products correctly`() {
        //given
        val filePath = "biedronka/paragon_2605070614124744.json"
        expectedProducts = loadExpectedProducts()
        val content = ResourceFileLoader.readTextUtf8(filePath)
        //when
        receiptData = BiedronkaParser().parse(content, "", "")

        //then
        assertEquals(expectedProducts.size, receiptData.products.size, "Różna liczba produktów")

        assertEquals("Biedronka", receiptData.store.name)
        assertEquals("2026-05-07", receiptData.receipt.date)
        assertEquals("19:59", receiptData.receipt.time)
        assertEquals(6577, receiptData.receipt.pln)
        assertEquals(504, receiptData.receipt.ptu)

        expectedProducts.forEachIndexed { i, expected ->
            val actual = receiptData.products[i]
            assertAll(
                "Produkt[$i] '${expected.name}'",
                { assertEquals(expected.name, actual.name, "name") },
                { assertEquals(expected.ptuType, actual.ptuType, "ptuType") },
                { assertEquals(expected.quantity, actual.quantity, "quantity") },
                { assertEquals(expected.unitPrice, actual.unitPrice, "priceUnit") },
                { assertEquals(expected.subtotalPrice, actual.subtotalPrice, "subtotalPrice") },
                { assertEquals(expected.discount, actual.discount, "discount") },
            )
        }
    }

    @Test
    fun `should parse store address correctly`() {
        //given
        val filePath = "biedronka/paragon_2605070614124744.json"
        val content = ResourceFileLoader.readTextUtf8(filePath)
        //when
        val result = BiedronkaParser().parse(content, "", "")
        //then
        assertAll(
            "Address",
            { assertEquals("60-149", result.address.postalCode, "postalCode") },
            { assertEquals("POZNAŃ", result.address.city, "city") },
            { assertEquals("GWIAŹDZISTA 30", result.address.streetName, "streetName") },
            { assertEquals(false, result.address.favourite, "favourite") },
            { assertEquals(false, result.address.autoSelect, "autoSelect") },
            { assertEquals("", result.address.id, "id") },
            { assertEquals("", result.address.storeId, "storeId") },
        )
    }

    private fun loadExpectedProducts(): List<ProductParsed> {
        val stream = Thread.currentThread().contextClassLoader
            .getResourceAsStream("biedronka/paragon_2605070614124744_expected.csv")
            ?: error("Nie znaleziono paragon_2605070614124744_expected.csv w resources")

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
                        finalPrice = subtotalPrice - discount
                    }
                }
        }
    }
}