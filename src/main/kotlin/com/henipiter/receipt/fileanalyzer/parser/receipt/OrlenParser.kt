package com.henipiter.receipt.fileanalyzer.parser.receipt


import com.henipiter.receipt.dto.PriceUtils
import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import com.henipiter.receipt.fileanalyzer.parser.util.Stores
import com.henipiter.receipt.parsed.AddressParsed
import com.henipiter.receipt.parsed.ProductParsed
import com.henipiter.receipt.parsed.ReceiptParsed
import com.henipiter.receipt.parsed.StoreParsed
import kotlin.math.roundToInt

class OrlenParser : ReceiptParser {

    override fun parse(content: String, storeId: String, defaultCategoryId: String): ReceiptData {
        val receiptStoreName = getStoreName(content)
        val receiptNip = getReceiptNip(content)
        val receiptDate = getReceiptDate(content)
        val receiptPrice = getReceiptPrice(content)
        val pair = analyzeProduct(content, defaultCategoryId)
        val address = getAddress(content, storeId)

        return ReceiptData(
            address = address,
            products = pair.first,
            receipt = ReceiptParsed().apply {
                this.storeId = storeId
                date = receiptDate
                time = "00:00"
                pln = receiptPrice
                ptu = pair.second
            },
            store = StoreParsed().apply {
                name = receiptStoreName
                nip = receiptNip
            }
        )
    }

    private fun getAddress(content: String, storeId: String): AddressParsed {
        val addresses = addressPattern.findAll(content).map { m ->
            if (m.groups["street1"] != null) {
                Triple(
                    m.groups["street1"]!!.value.trim(),
                    m.groups["postal1"]!!.value.trim(),
                    m.groups["city1"]!!.value.trim()
                )
            } else {
                Triple(
                    m.groups["street2"]!!.value.trim(),
                    m.groups["postal2"]!!.value.trim(),
                    m.groups["city2"]!!.value.trim()
                )
            }
        }.toList()

        if (addresses.isEmpty()) return AddressParsed()

        val (street, postal, city) = if (addresses.size >= 2) addresses[1] else addresses[0]

        return AddressParsed(
            "",
            storeId = storeId,
            city = city,
            postalCode = postal,
            streetName = street,
            favourite = false,
            autoSelect = true
        )
    }

    private fun getReceiptPrice(content: String): Int {
        var receiptPriceMatch = receiptPriceMatch.find(content)
        if (receiptPriceMatch == null) {
            receiptPriceMatch = receiptPriceMatchSecond.find(content)
        }
        val price =
            receiptPriceMatch?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        return (price * 100).roundToInt()
    }

    private fun getReceiptDate(content: String): String {
        val dateMatch = dateRegex.find(content)
        return dateMatch?.groupValues?.get(1)?.trim() ?: ""

    }

    private fun getReceiptNip(content: String): String {
        val nipMatch = nipRegex.find(content)
        return nipMatch?.groupValues?.get(1)?.trim()?.replace("-", "") ?: ""

    }

    private fun getStoreName(content: String): String {
        val storeNameMatch = storeNameRegex.find(content)
        return storeNameMatch?.groupValues?.get(1)?.trim() ?: Stores.ORLEN.storeName
    }

    private fun toInt(text: String): Int {
        val double = text.replace(",", ".").toDouble()
        return (double * 100).roundToInt()
    }

    private fun analyzeProduct(
        content: String,
        defaultCategoryId: String
    ): Pair<MutableList<ProductParsed>, Int> {

        val products = mutableListOf<ProductParsed>()
        var receiptTax = 0

        for (result in productPattern.findAll(content)) {
            val productName = result.groupValues[2].trim()
            val productQuantity =
                PriceUtils.doubleQuantityTextToInt(result.groupValues[4].replace(",", "."))
            val productPriceUnit =
                PriceUtils.doublePriceTextToInt(result.groupValues[5].replace(",", "."))
            val productDiscount =
                PriceUtils.doublePriceTextToInt(result.groupValues[6].replace(",", "."))
            val productFinalPrice =
                PriceUtils.doublePriceTextToInt(result.groupValues[11].replace(",", "."))
            val productTaxType = result.groupValues[9]
            val productVatAmount =
                PriceUtils.doublePriceTextToInt(result.groupValues[10].replace(",", "."))

            receiptTax += productVatAmount

            products.add(
                ProductParsed().apply {
                    name = productName
                    quantity = productQuantity
                    unitPrice = productPriceUnit
                    subtotalPrice = productFinalPrice + productDiscount
                    discount = productDiscount
                    ptuType = "A"
                    finalPrice = productFinalPrice
                    categoryId = defaultCategoryId
                    validPrice = true
                })
        }
        return Pair(products, receiptTax)
    }

    companion object {
        val productPattern = Regex(
            """^(\d+)\s+(.+?)\s+(L|kg|szt|ml|g)\s+""" +
                    """([\d]+[,.][\d]+)\s+""" + // ilość
                    """([\d]+[,.][\d]+)\s+""" + // cena brutto
                    """([\d]+[,.][\d]+)\s+""" + // wartość rabatu
                    """([\d]+[,.][\d]+)\s+""" + // cena brutto po rabacie
                    """([\d]+[,.][\d]+)\s+""" + // wartość netto
                    """(\d+)\s+""" + // VAT%
                    """([\d]+[,.][\d]+)\s+""" + // kwota VAT
                    """([\d]+[,.][\d]+)""", // wartość brutto
            RegexOption.MULTILINE
        )

        val storeNameRegex = Regex("""Wystawca:\s*(.+)""")
        val nipRegex = Regex("""NIP[:\s]+([\d\-]+)""")
        val dateRegex = Regex("""Data wystawienia[:\s]+(\d{4}-\d{2}-\d{2})""")
        val receiptPriceMatch = Regex("""Należność ogółem[:\s]+([\d]+[.,][\d]{2})\s*PLN""")
        val receiptPriceMatchSecond = Regex("""Nale.no.. og.?em[:\s]+([\d]+[.,][\d]{2})\s*PLN""")

        val addressPattern = Regex(
            """Adres:\s*(?<street1>.+?)\s+(?<postal1>\d{2}-\d{3})\s+(?<city1>\S+)""" +
                    """|(?<postal2>\d{2}-\d{3})\s+(?<city2>\S+)\s*\n\s*ul\.?\s*(?<street2>.+)"""
        )
    }
}