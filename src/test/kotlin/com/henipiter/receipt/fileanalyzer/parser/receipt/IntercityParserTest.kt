package com.henipiter.receipt.fileanalyzer.parser.receipt

import com.henipiter.receipt.parsed.ProductParsed
import com.henipiter.receipt.util.ResourceFileLoader
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntercityParserTest {


    @Test
    fun `should parse whole ticket correctly for june`() {
        //given
        val filePath = "intercity/bilet_WH_56409985_pl.txt"
        val text = ResourceFileLoader.readTextUtf8(filePath)
        val expectedProducts = listOf(
            ProductParsed().apply {
                name = "Bilet normalny (Poznań Gł. Choroszcz)"
                ptuType = "A"
                unitPrice = 1900
                finalPrice = 1900
            },
            ProductParsed().apply {
                name = "STU/DOK (Poznań Gł. Choroszcz)"
                ptuType = "A"
                unitPrice = 931
                finalPrice = 931
            }
        )

        //when
        val receiptData = IntercityParser().parse(text, "", "")

        //then
        assertEquals("Intercity", receiptData.store.name)
        assertEquals("2026-01-03", receiptData.receipt.date)
        assertEquals("15:37", receiptData.receipt.time)
        assertEquals("5262544258", receiptData.store.nip)
        assertEquals(2831, receiptData.receipt.pln)
        assertEquals(210, receiptData.receipt.ptu)

        assertEquals(expectedProducts.size, receiptData.products.size, "Różna liczba produktów")

        expectedProducts.forEachIndexed { i, expected ->
            val actual = receiptData.products[i]
            assertAll(
                "Produkt[$i] '${expected.name}'",
                { assertEquals(expected.name, actual.name, "name") },
                { assertEquals(expected.ptuType, actual.ptuType, "taxType") },
                { assertEquals(expected.unitPrice, actual.unitPrice, "priceUnit") },
                { assertEquals(expected.finalPrice, actual.finalPrice, "finalPrice") },
            )
        }
    }


    @Test
    fun `should parse whole ticket correctly for july`() {
        //given
        val filePath = "intercity/bilet_WH_65571305_en.txt"
        val text = ResourceFileLoader.readTextUtf8((filePath))
        val expectedProducts = listOf(
            ProductParsed().apply {
                name = "Adult (Poznań Gł. Warszawa Gł.)"
                ptuType = "A"
                unitPrice = 1600
                finalPrice = 1600
            }
        )

        //when
        val receiptData = IntercityParser().parse(text, "", "")

        //then
        assertEquals("Intercity", receiptData.store.name)
        assertEquals("2026-01-06", receiptData.receipt.date)
        assertEquals("21:02", receiptData.receipt.time)
        assertEquals("5262544258", receiptData.store.nip)
        assertEquals(1600, receiptData.receipt.pln)
        assertEquals(119, receiptData.receipt.ptu)

        assertEquals(expectedProducts.size, receiptData.products.size, "Różna liczba produktów")

        expectedProducts.forEachIndexed { i, expected ->
            val actual = receiptData.products[i]
            assertAll(
                "Produkt[$i] '${expected.name}'",
                { assertEquals(expected.name, actual.name, "name") },
                { assertEquals(expected.ptuType, actual.ptuType, "taxType") },
                { assertEquals(expected.unitPrice, actual.unitPrice, "priceUnit") },
                { assertEquals(expected.finalPrice, actual.finalPrice, "finalPrice") },
            )
        }
    }


}