package com.henipiter.receipt.exception

import com.henipiter.receipt.base.ReceiptBase

class ReceiptAlreadyExistsException(val receipt: ReceiptBase) :
    Exception("Receipt already exists: ${receipt.date} ${receipt.time} ${receipt.pln}")