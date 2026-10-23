package com.henipiter.receipt.base

interface PayerBase {
    var id: String
    var name: String
    var defaultBeneficiaryId: String
    var defaultPayer: Boolean
}
