package com.henipiter.receipt.save

import com.henipiter.receipt.base.StoreBase
import com.henipiter.receipt.exception.StoreNotFoundException
import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import com.henipiter.receipt.fileanalyzer.parser.receipt.ReceiptParserFactory
import com.henipiter.receipt.fileanalyzer.parser.util.Stores

class ParseReceipt(
    val factory: ReceiptParserFactory,
    val getStoreByNip: suspend (nip: String) -> StoreBase?,
) {

    suspend fun parse(store: Stores, bytes: ByteArray, nip: String, categoryId: String?): ReceiptData {
        val content = factory.getContent(store, bytes)
        val storeDb = getStoreByNip(nip)
        val storeId = storeDb?.id ?: ""
        val categoryId = storeDb?.defaultCategoryId ?: categoryId ?: ""

        val receiptData = factory.getParser(store).parse(content, storeId, categoryId)

        if (storeDb == null && categoryId.isEmpty()) {
            throw StoreNotFoundException(receiptData.store.nip)
        }
        return receiptData
    }

}