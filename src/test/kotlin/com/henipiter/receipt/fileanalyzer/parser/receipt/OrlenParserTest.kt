package com.henipiter.receipt.fileanalyzer.parser.receipt

import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import com.henipiter.receipt.parsed.ProductParsed
import com.henipiter.receipt.util.ResourceFileLoader
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.InputStreamReader

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrlenParserTest {

    private lateinit var receiptData: ReceiptData
    private lateinit var expectedProducts: List<ProductParsed>


    @Test
    fun `should parse whole receipt correctly txt`() {
        //given
        val filePath = "orlen/FakturaF10664_0832_26OrlenPay.txt"
        val text = ResourceFileLoader.readTextUtf8(filePath)
        expectedProducts = loadExpectedProducts()

        //when
        receiptData = OrlenParser().parse(text, "", "")

        //then
        assertAll(
            { assertEquals("ORLEN S.A.", receiptData.store.name) },
            { assertEquals("2026-01-05", receiptData.receipt.date) },
            { assertEquals("00:00", receiptData.receipt.time) },
            { assertEquals("7740001454", receiptData.store.nip) },
            { assertEquals(13615, receiptData.receipt.pln) },
            { assertEquals(2546, receiptData.receipt.ptu) }
        )

        assertAll(
            "Address",
            { assertEquals("89-500", receiptData.address.postalCode, "postalCode") },
            { assertEquals("Tuchola", receiptData.address.city, "city") },
            { assertEquals("Chojnicka nr 75", receiptData.address.streetName, "streetName") },
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
            .getResourceAsStream("orlen/FakturaF10664_0832_26OrlenPay_expected.csv")
            ?: error("Nie znaleziono FakturaF10664_0832_26OrlenPay_expected.csv w resources")

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
                        unitPrice = cols[2].toInt()
                        quantity = cols[3].toInt()
                        subtotalPrice = cols[4].toInt()
                        discount = cols[5].toInt()
                        finalPrice = cols[6].toInt()
                    }
                }
        }
    }
}