package com.henipiter.receipt.fileanalyzer.parser.receipt

import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData

interface ReceiptParser {

    fun parse(content: String, storeId: String, defaultCategoryId: String): ReceiptData
}