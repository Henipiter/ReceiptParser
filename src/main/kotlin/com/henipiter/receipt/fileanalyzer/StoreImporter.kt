package com.henipiter.receipt.fileanalyzer

import com.henipiter.receipt.fileanalyzer.parser.util.Stores
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

enum class PayloadType { JSON, HTML, PDF_BASE64 }

data class RawReceipt(
    val store: Stores,
    val externalId: String,
    val sourceUrl: String,
    val type: PayloadType,
    val payload: String
) {
    @OptIn(ExperimentalEncodingApi::class)
    fun bytes(): ByteArray = when (type) {
        PayloadType.PDF_BASE64 -> Base64.Mime.decode(payload)
        else -> payload.toByteArray(Charsets.UTF_8)
    }
}

interface StoreImporter {
    val store: Stores
    val startUrl: String
    val allowedHosts: Set<String>
    val loginHosts: Set<String> get() = emptySet()
    val scriptAsset: String

    fun shouldInject(url: String): Boolean

    fun isHostAllowed(host: String?): Boolean = matches(host, allowedHosts)

    fun isNavigationAllowed(host: String?): Boolean =
        matches(host, allowedHosts) || matches(host, loginHosts)

    private fun matches(host: String?, set: Set<String>) =
        host != null && set.any { host == it || host.endsWith(".$it") }
}