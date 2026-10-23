package com.henipiter.receipt.base

data class Address (
    var id: String = "",
    var storeId: String = "",
    var city: String = "",
    var postalCode: String = "",
    var streetName: String = "",
    var autoSelect: Boolean = false,
    var favourite: Boolean=false
)
