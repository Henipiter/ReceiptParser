package com.henipiter.receipt.fileanalyzer.parser.receipt


import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import com.henipiter.receipt.fileanalyzer.parser.util.Stores
import com.henipiter.receipt.parsed.AddressParsed
import com.henipiter.receipt.parsed.ProductParsed
import com.henipiter.receipt.parsed.ReceiptParsed
import com.henipiter.receipt.parsed.StoreParsed
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.math.round
import kotlin.math.roundToInt

class LidlParser : ReceiptParser {

    override fun parse(content: String, storeId: String, defaultCategoryId: String): ReceiptData {
        val soup = Jsoup.parse(content)

        val purchaseList = soup.select("[class~=purchase_list]").firstOrNull()
        val receiptDate = getReceiptDate(purchaseList)
        val receiptTime = getReceiptTime(soup)
        val products = analyzeProducts(purchaseList, defaultCategoryId)
        val receiptPrice = getReceiptPrice(soup)
        val receiptTax = getReceiptTax(soup)
        val address = getAddress(soup, storeId)

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
                name = Stores.LIDL.storeName
                nip = Stores.LIDL.nip
            }
        )
    }

    private fun getReceiptTime(soup: Document): String {
        val summaryLine = soup.select("#purchase_summary_2").firstOrNull()?.text() ?: ""
        val match = Regex("""(\d{2}):(\d{2})""").find(summaryLine)
        return match?.value ?: "00:00"
    }

    private fun getAddress(soup: Document, storeId: String): AddressParsed {
        val line = soup.select("#header_line_6").firstOrNull()?.text()?.trim() ?: ""
        if (line.isBlank()) {
            return AddressParsed()
        }

        val parts = line.split(",")
        if (parts.size < 2) {
            return AddressParsed()
        }

        val streetName = parts[0].trim().replace(Regex("(?i)^ul\\.\\s*"), "").trim()
        val postalAndCity = parts[1].trim()

        val match = Regex("""(\d{2}-\d{3})\s+(.+)""").find(postalAndCity)
        val postalCode = match?.groupValues?.get(1) ?: ""
        val city = match?.groupValues?.get(2)?.trim() ?: ""

        return AddressParsed(
            "",
            storeId = storeId,
            city = city,
            postalCode = postalCode,
            streetName = streetName,
            favourite = false,
            autoSelect = true
        )
    }

    private fun analyzeProducts(
        purchaseList: Element?,
        defaultCategoryId: String
    ): MutableList<ProductParsed> {
        val products = mutableListOf<ProductParsed>()
        purchaseList?.children()?.forEach { span ->
            val classes = span.classNames()
            when {
                "article" in classes && span.hasAttr("data-art-description") -> {
                    calculateArticle(span, products, defaultCategoryId)
                }

                "discount" in classes -> {
                    calculateDiscount(span, products)
                }
            }
        }
        return products
    }

    private fun getReceiptDate(purchaseList: Element?): String {

        purchaseList?.select("span.currency")?.firstOrNull()?.let {
            return it.text().trim()
        }
        return ""
    }

    private fun getReceiptPrice(soup: Document): Int {
        var receiptPrice = 0
        val summary = soup.select("[class~=purchase_summary]").firstOrNull()
        summary?.text()?.split("\n")?.forEach { line ->
            if ("Suma PLN" in line || "Suma " in line) {
                val match = Regex("""Suma(?: PLN)?\s+([\d,]+)""").find(line)
                match?.let {
                    receiptPrice = getIntValue(it)
                    return@forEach
                }
            }
        }
        return receiptPrice
    }

    private fun getReceiptTax(soup: Document): Int {
        var receiptTax = 0
        val vatInfo = soup.select("[class~=vat_info]").firstOrNull()
        vatInfo?.children()?.forEach { child ->
            if (child.tagName() == "span" && "Suma" in child.text()) {
                val match = Regex("""Suma\s+([\d,]+)""").find(child.text())
                match?.let {
                    receiptTax = getIntValue(it)
                }
            }
        }
        return receiptTax

    }

    private fun getIntValue(result: MatchResult): Int {
        val value = result.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
        return (value * 100).roundToInt()
    }

    private fun sameAsBefore(
        last: ProductParsed,
        productName: String,
        productQuantity: Int,
        priceUnit: Int
    ): Boolean =
        last.name == productName && last.quantity == productQuantity && last.unitPrice == priceUnit

    private fun calculateArticle(
        span: Element,
        products: MutableList<ProductParsed>,
        defaultCategoryId: String
    ) {
        val productName = span.attr("data-art-description").trim()
        val productQuantity = getProductQuantity(span)
        val priceUnit = getProductPriceUnit(span)
        val subtotal = round(productQuantity * priceUnit / 1000.0).toInt()
        val taxType = span.attr("data-tax-type")

        val last = products.lastOrNull()
        if (last == null || !sameAsBefore(last, productName, productQuantity, priceUnit)) {
            products.add(
                ProductParsed().apply {
                    name = productName
                    quantity = productQuantity
                    unitPrice = priceUnit
                    subtotalPrice = subtotal
                    discount = 0
                    ptuType = taxType
                    finalPrice = subtotal
                    categoryId = defaultCategoryId
                    validPrice = true
                })
        }
    }

    private fun getProductQuantity(span: Element): Int {
        val productQuantity =
            span.attr("data-art-quantity").replace(",", ".").toDoubleOrNull() ?: 1.0
        return (productQuantity * 1000).roundToInt()
    }

    private fun getProductPriceUnit(span: Element): Int {
        val productPriceUnit =
            span.attr("data-unit-price").replace(",", ".").toDoubleOrNull() ?: 0.0
        return (productPriceUnit * 100).roundToInt()
    }

    private fun calculateDiscount(span: Element, products: MutableList<ProductParsed>) {
        val match = Regex("""-([\d,]+)""").find(span.text().trim())
        match?.let {
            products.lastOrNull()?.let { prod ->
                prod.discount += getIntValue(it)
                prod.finalPrice = prod.subtotalPrice - prod.discount
            }
        }
    }
}