package com.henipiter.receipt.base

interface AddressBase {
    var id: String
    var storeId: String
    var city: String
    var postalCode: String
    var streetName: String
    var autoSelect: Boolean
    var favourite: Boolean
}
