package com.henipiter.receipt.parsed

import com.henipiter.receipt.base.FeedbackBase

data class FeedbackParsed(
    override var id: String = "",
    override var message: String = "",
    override var response: String = "",
    override var seen: Boolean = false
) : FeedbackBase
