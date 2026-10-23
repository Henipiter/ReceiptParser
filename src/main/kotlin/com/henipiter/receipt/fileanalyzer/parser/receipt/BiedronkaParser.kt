package com.henipiter.receipt.fileanalyzer.parser.receipt

import com.henipiter.receipt.parsed.AddressParsed
import com.henipiter.receipt.parsed.ProductParsed
import com.henipiter.receipt.parsed.ReceiptParsed
import com.henipiter.receipt.parsed.StoreParsed
import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import com.henipiter.receipt.fileanalyzer.parser.util.Stores
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class BiedronkaParser : ReceiptParser {

    override fun parse(content: String, storeId: String, defaultCategoryId: String): ReceiptData {
        val data = Json.parseToJsonElement(content).jsonObject

        var receiptDate = ""
        var receiptTime = "00:00"
        var address = AddressParsed()
        val header = data["header"]?.jsonArray
        if (header != null) {
            for (element in header) {
                val item = element.jsonObject
                if ("headerData" in item) {
                    val rawDate =
                        item["headerData"]?.jsonObject?.get("date")?.jsonPrimitive?.contentOrNull
                    receiptDate = extractDate(rawDate)
                    receiptTime = extractTime(rawDate)
                }
                if ("headerText" in item) {
                    val headerTextLines =
                        item["headerText"]?.jsonObject?.get("headerTextLines")?.jsonPrimitive?.contentOrNull
                            ?: ""
                    address = parseAddress(headerTextLines, storeId)
                }
            }
        }

        val products = mutableListOf<ProductParsed>()

        var receiptPrice = 0
        var receiptTax = 0
        val body = data["body"]?.jsonArray
        if (body != null) {
            val pair = readProducts(body, products, defaultCategoryId)
            receiptPrice = pair.first
            receiptTax = pair.second
        }

        return ReceiptData(
            address = address,
            products = products,
            receipt = ReceiptParsed().apply {
                this.storeId = storeId
                date = receiptDate
                time = receiptTime
                pln = receiptPrice
                ptu = receiptTax
            },
            store = StoreParsed().apply {
                name = Stores.BIEDRONKA.storeName
                nip = Stores.BIEDRONKA.nip
            }
        )
    }

    private fun extractDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""
        return rawDate.substringBefore("T")
    }

    private fun extractTime(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "00:00"

        return runCatching {
            val instant = Instant.parse(rawDate)

            val warsawZone = ZoneId.of("Europe/Warsaw")
            val localDateTime = instant.atZone(warsawZone)

            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            localDateTime.format(formatter)
        }.getOrDefault("00:00")
    }

    private fun parseAddress(headerTextLines: String, storeId: String): AddressParsed {
        val divs = Regex("<div[^>]*>(.*?)</div>").findAll(headerTextLines)
            .map { it.groupValues[1] }
            .toList()

        if (divs.size < 2) {
            return AddressParsed()
        }

        val storeAddressLine = divs[1]
            .replace("&nbsp;", " ")
            .replace(Regex("<[^>]*>"), "")
            .trim()

        val parts = storeAddressLine.split(Regex("(?i)UL\\."), limit = 2)
        if (parts.size < 2) {
            return AddressParsed()
        }

        val postalAndCity = parts[0].trim().split(" ", limit = 2)
        val postalCode = postalAndCity.getOrElse(0) { "" }
        val city = postalAndCity.getOrElse(1) { "" }.trim()

        val streetAndNr = parts[1].trim()
        val streetTokens = streetAndNr.split(" ")
        val nr = streetTokens.lastOrNull() ?: ""
        val streetOnly = streetTokens.dropLast(1).joinToString(" ")
        val streetName = if (nr.isNotBlank()) {
            "$streetOnly $nr".trim()
        } else {
            streetOnly
        }

        return AddressParsed(
            city = city,
            postalCode = postalCode,
            streetName = streetName,
            storeId = storeId,
            favourite = false,
            autoSelect = false
        )
    }

    private fun readProducts(
        body: JsonArray,
        products: MutableList<ProductParsed>,
        defaultCategoryId: String
    ): Pair<Int, Int> {
        var receiptPrice = 0
        var receiptTax = 0
        for (element in body) {
            val item = element.jsonObject
            when {
                "sumInCurrency" in item -> {
                    receiptPrice += calculateReceiptPrice(item)
                }

                "vatSummary" in item -> {
                    receiptTax += calculateVatSummary(item)
                }

                "sellLine" in item -> {
                    products.add(calculateSellLine(item, defaultCategoryId))
                }

                "discountLine" in item -> {
                    calculateDiscountLine(item, products)
                }
            }
        }
        return Pair(receiptPrice, receiptTax)
    }

    private fun stringToQuantity(text: String): Int {
        val double = text.replace(",", ".").toDoubleOrNull() ?: 1.0
        return (double * 1000).roundToInt()
    }

    private fun calculateSellLine(item: JsonObject, defaultCategoryId: String): ProductParsed {
        val sl = item["sellLine"]?.jsonObject
        val subtotalPriceValue = sl?.get("total")?.jsonPrimitive?.intOrNull ?: 0
        val quantityValue =
            stringToQuantity(sl?.get("quantity")?.jsonPrimitive?.contentOrNull ?: "1")

        return ProductParsed().apply {
            name = sl?.get("name")?.jsonPrimitive?.contentOrNull?.trim() ?: ""
            quantity = quantityValue
            unitPrice = sl?.get("price")?.jsonPrimitive?.intOrNull ?: 0
            subtotalPrice = subtotalPriceValue
            discount = 0
            ptuType = sl?.get("vatId")?.jsonPrimitive?.contentOrNull ?: ""
            finalPrice = subtotalPriceValue
            categoryId = defaultCategoryId
            validPrice = true
        }
    }

    private fun calculateDiscountLine(item: JsonObject, products: List<ProductParsed>) {
        val dl = item["discountLine"]?.jsonObject ?: return
        products.lastOrNull()?.let {
            it.discount += dl["value"]?.jsonPrimitive?.intOrNull ?: 0
            it.finalPrice = it.subtotalPrice - it.discount
        }
    }

    private fun calculateReceiptPrice(item: JsonObject): Int {
        return item["sumInCurrency"]?.jsonObject?.get("fiscalTotal")?.jsonPrimitive?.intOrNull ?: 0
    }

    private fun calculateVatSummary(item: JsonObject): Int {
        var receiptTax = 0
        val vatRates = item["vatSummary"]?.jsonObject?.get("vatRatesSummary")?.jsonArray
        if (vatRates != null) {
            for (element in vatRates) {
                receiptTax += element.jsonObject["vatAmount"]?.jsonPrimitive?.intOrNull ?: 0
            }
        }
        return receiptTax
    }
}