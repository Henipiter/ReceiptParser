package com.henipiter.receipt.fileanalyzer.parser.receipt

import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import com.henipiter.receipt.fileanalyzer.parser.util.Stores
import com.henipiter.receipt.parsed.AddressParsed
import com.henipiter.receipt.parsed.ProductParsed
import com.henipiter.receipt.parsed.ReceiptParsed
import com.henipiter.receipt.parsed.StoreParsed
import kotlin.math.roundToInt

class IntercityParser : ReceiptParser {

    override fun parse(content: String, storeId: String, defaultCategoryId: String): ReceiptData {
        val receiptDate = getReceiptDate(content)
        val receiptTime = getReceiptTime(content)
        val receiptPrice = getReceiptPrice(content)
        val receiptTax = getReceiptTax(content)
        val route = getRoute(content)
        val ticketEntries = extractTicketEntries(content)
        val products = buildProducts(ticketEntries, receiptPrice, defaultCategoryId, route)

        return ReceiptData(
            address = AddressParsed(
                "",
                storeId,
                "Warszawa",
                "02-305",
                "Aleje Jerozolimskie 142A",
                false,
                true
            ),
            products = products,
            receipt = ReceiptParsed().apply {
                this.storeId = storeId
                date = receiptDate
                time = receiptTime
                pln = receiptPrice
                ptu = receiptTax
                online = true
            },
            store = StoreParsed().apply {
                name = Stores.INTERCITY.storeName
                nip = Stores.INTERCITY.nip
            }
        )
    }

    private fun toInt(text: String): Int {
        val double = text.replace(",", ".").toDouble()
        return (double * 100).roundToInt()
    }

    private fun getReceiptDate(content: String): String {
        val match = purchaseDateTimeRegex.find(content) ?: return ""
        val (day, month, year) = Triple(
            match.groupValues[1],
            match.groupValues[2],
            match.groupValues[3]
        )
        return "$year-$month-$day"
    }

    private fun getReceiptTime(content: String): String {
        val match = purchaseDateTimeRegex.find(content) ?: return "00:00"
        return match.groupValues[4]
    }

    private fun getReceiptPrice(content: String): Int {
        val match = totalPriceRegex.find(content) ?: return 0
        return toInt(match.groupValues[1])
    }

    private fun getReceiptTax(content: String): Int {
        val match = vatAmountRegex.find(content) ?: return 0
        return toInt(match.groupValues[1])
    }

    private fun getRoute(content: String): String {
        val match = routeRegex.find(content) ?: return ""
        return match.groupValues[1].trim()
    }

    data class TicketEntry(val name: String, val quantity: Int, val discountPercent: Int)

    /**
     * Nazwy i ilości biletów bierzemy z sekcji "A-Cena bazowa" (stabilna, bez nazw stacji).
     * Procent ulgi bierzemy z górnej sekcji "BILET" (tam, gdzie faktycznie występuje "Ulga X%"),
     * dopasowując po nazwie biletu, żeby nie polegać na nazwie stacji jako granicy sekcji.
     */
    private fun extractTicketEntries(content: String): List<TicketEntry> {
        val baseEntries = extractBaseEntries(content)
        val discountMap = extractDiscountMap(content)

        return baseEntries.map { entry ->
            entry.copy(discountPercent = discountMap[entry.name] ?: 0)
        }
    }

    private fun extractBaseEntries(content: String): List<TicketEntry> {
        val sectionMatch = baseSectionRegex.find(content) ?: return emptyList()
        val sectionText = sectionMatch.groupValues[1]

        return sectionText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val m = ticketLineRegex.find(line) ?: return@mapNotNull null
                TicketEntry(
                    name = m.groupValues[2].trim(),
                    quantity = m.groupValues[1].toInt(),
                    discountPercent = 0
                )
            }
    }

    private fun extractDiscountMap(content: String): Map<String, Int> {
        val biletIndex = content.indexOf("BILET")
        if (biletIndex == -1) return emptyMap()

        val window = content.substring(biletIndex, minOf(biletIndex + 300, content.length))

        val discounts = mutableMapOf<String, Int>()
        var lastName: String? = null

        window.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val ticketMatch = ticketLineRegex.find(line)
                val discountMatch = discountLineRegex.find(line)

                when {
                    discountMatch != null && lastName != null -> {
                        discounts[lastName!!] = discountMatch.groupValues[1].toInt()
                        lastName = null
                    }

                    ticketMatch != null -> {
                        lastName = ticketMatch.groupValues[2].trim()
                    }
                }
            }

        return discounts
    }

    private fun buildProducts(
        entries: List<TicketEntry>,
        totalPrice: Int,
        defaultCategoryId: String,
        route: String
    ): MutableList<ProductParsed> {
        if (entries.isEmpty()) {
            return mutableListOf()
        }

        val weights = entries.map { it.quantity * (100 - it.discountPercent) }
        val totalWeight = weights.sum()

        val products = mutableListOf<ProductParsed>()
        var allocated = 0

        entries.forEachIndexed { index, entry ->
            val groupPrice = if (index < entries.size - 1) {
                (totalPrice.toDouble() * weights[index] / totalWeight).roundToInt()
            } else {
                totalPrice - allocated
            }
            allocated += groupPrice

            val unit = (groupPrice.toDouble() / entry.quantity).roundToInt()
            val fullName = if (route.isNotBlank()) "${entry.name} ($route)" else entry.name

            products.add(
                ProductParsed().apply {
                    name = fullName
                    quantity = entry.quantity * 1000
                    unitPrice = unit
                    subtotalPrice = groupPrice
                    discount = 0
                    ptuType = "A"
                    finalPrice = groupPrice
                    categoryId = defaultCategoryId
                    validPrice = true
                }
            )
        }

        return products
    }

    companion object {
        private const val NL = """[\r\n]+"""

        val purchaseDateTimeRegex = Regex(
            """(?:Data i godzina zakupu|Purchase time)\s*$NL\s*(\d{2})\.(\d{2})\.(\d{4})\s+(\d{2}:\d{2})"""
        )

        val totalPriceRegex = Regex(
            """(?:Razem\s*$NL\s*Kwota|TOTAL\s*$NL\s*PRICE)\s*\(zł\)\s*$NL\s*(\d+[,.]\d{2})\s*zł"""
        )

        val vatAmountRegex = Regex(
            """VAT\s*\(zł\)\s*$NL\s*\d+%\s+(\d+[,.]\d{2})"""
        )

        val baseSectionRegex = Regex(
            """(?m)^[ \t]*(?:Bilet|Ticket)[ \t]*\r?\n(.*?)\r?\n[ \t]*VAT""",
            RegexOption.DOT_MATCHES_ALL
        )

        val routeRegex = Regex(
            """[\r\n]([^\r\n]+?)[\r\n]+\s*(?:Data odjazdu\s*/\s*przyjazdu|Departure\s*/\s*arrival date)"""
        )

        val ticketHeaderRegex = Regex("""(?m)^[ \t]*(?:BILET|TICKET)[ \t]*\r?$""")

        val ticketLineRegex = Regex("""(\d+)x\s+(.+)""")
        val discountLineRegex = Regex("""(?:Ulga|Discount)\s*(\d+)\s*%""", RegexOption.IGNORE_CASE)
    }
}