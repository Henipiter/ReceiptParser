package com.henipiter.receipt.fileanalyzer.parser.receipt


import com.henipiter.receipt.fileanalyzer.parser.util.Stores

abstract class ReceiptParserFactory {

    fun getParser(sourceStore: Stores): ReceiptParser {
        return when (sourceStore) {
            Stores.ORLEN -> OrlenParser()
            Stores.BIEDRONKA -> BiedronkaParser()
            Stores.LIDL -> LidlParser()
            Stores.INTERCITY -> IntercityParser()
        }
    }

    abstract fun getContent(sourceStore: Stores, bytes: ByteArray): String
}