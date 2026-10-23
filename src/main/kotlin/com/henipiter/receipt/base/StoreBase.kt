package com.henipiter.receipt.base

import com.henipiter.receipt.dto.ReceiverType

interface StoreBase {
    var id: String
    var nip: String
    var name: String
    var receiverType: ReceiverType
    var addressId: String
    var defaultCategoryId: String
}
