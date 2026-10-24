package com.henipiter.receipt.save

import com.henipiter.receipt.exception.StoreNotFoundException
import com.henipiter.receipt.base.AddressBase
import com.henipiter.receipt.base.ProductBase
import com.henipiter.receipt.base.ReceiptBase
import com.henipiter.receipt.base.StoreBase
import com.henipiter.receipt.dto.ReceiptExtras
import com.henipiter.receipt.exception.ReceiptAlreadyExistsException
import com.henipiter.receipt.fileanalyzer.parser.model.ReceiptData
import java.util.*

class SaveReceiptFlow(
    val getStoreByNip: suspend (nip: String) -> StoreBase?,
    val checkIsReceiptUnique: suspend (receipt: ReceiptBase) -> Boolean,
    val insertSingleProduct: suspend (product: ProductBase) -> Unit,
    val insertStore: suspend (store: StoreBase) -> String,
    val insertReceipt: suspend (receipt: ReceiptBase) -> String,
    val insertAddress: suspend (addressBase: AddressBase) -> String,
    val getAddressById: suspend (addressId: String) -> AddressBase?,
    val getAddressByPostalCode: suspend (postalCode: String, storeId: String) -> AddressBase?,
) {

    suspend fun saveData(receiptData: ReceiptData, extras: ReceiptExtras) {
        try {
            validateParsedReceiptBeforeSave(receiptData)
        } catch (_: Exception) {
            return
        }

        var store = getStoreByNip(receiptData.store.nip)

        if (store == null && !extras.categoryId.isNullOrEmpty()) {
            addStore(receiptData, extras.categoryId)
        } else if (store == null) {
            throw StoreNotFoundException(receiptData.store.nip)
        }

        store = getStoreByNip(receiptData.store.nip)

        receiptData.address.storeId = store!!.id
        receiptData.receipt.storeId = store.id
        receiptData.store = store

        if (!checkIsReceiptUnique(receiptData.receipt)) {
            throw ReceiptAlreadyExistsException(receiptData.receipt)
        }

        updateCategoryId(receiptData)
        addAddress(receiptData)
        addReceipt(receiptData, extras.payerId, extras.beneficiaryId)
        addProducts(receiptData)
    }


    private fun updateCategoryId(receiptData: ReceiptData) {
        receiptData.products.forEach { product ->
            product.categoryId = receiptData.store.defaultCategoryId
        }
    }

    private suspend fun addProducts(receiptData: ReceiptData) {
        receiptData.products.forEach { product ->
            product.receiptId = receiptData.receipt.id
            product.beneficiaryId = receiptData.receipt.beneficiaryId

            insertSingleProduct(product)
        }
    }

    private suspend fun addStore(receiptData: ReceiptData, categoryId: String) {
        receiptData.store.defaultCategoryId = categoryId
        receiptData.store.addressId = UUID.randomUUID().toString()
        receiptData.store.id = insertStore(receiptData.store)

        receiptData.address.id = receiptData.store.addressId
        receiptData.address.storeId = receiptData.store.id

    }

    private suspend fun addReceipt(
        receiptData: ReceiptData,
        payerId: String,
        beneficiaryId: String
    ) {
        receiptData.receipt.addressId = receiptData.address.id
        receiptData.receipt.payerId = payerId
        receiptData.receipt.beneficiaryId = beneficiaryId
        receiptData.receipt.id = insertReceipt(receiptData.receipt)

    }

    private suspend fun addAddress(receiptData: ReceiptData) {
        if (receiptData.receipt.online) {
            val address = getAddressById(receiptData.store.addressId)

            if (address != null) {
                receiptData.address = address
                return
            }

            receiptData.address.id = receiptData.store.addressId
            receiptData.address.storeId = receiptData.store.id
        }

        val address = getAddressByPostalCode(receiptData.address.postalCode, receiptData.store.id)

        if (address != null) {
            receiptData.address = address
            return
        }

        receiptData.address.id = insertAddress(receiptData.address)
    }

    fun validateParsedReceiptBeforeSave(receiptData: ReceiptData) {
        receiptData.receipt.apply {
            if (date.isEmpty() || time.isEmpty() || pln == 0) {
                throw Exception("Invalid receipt data")
            }
        }
        receiptData.address.apply {
            if (city.isEmpty() || postalCode.isEmpty() || streetName.isEmpty()) {
                throw Exception("Invalid receipt data")
            }
        }
        receiptData.products.forEach { product ->
            if (product.name.isEmpty() || product.quantity == 0 || product.unitPrice == 0 || product.subtotalPrice == 0 || product.finalPrice == 0) {
                throw Exception("Invalid receipt data")
            }
        }

    }

}