package com.henipiter.receipt.fileanalyzer

import kotlin.io.encoding.Base64
import com.henipiter.receipt.fileanalyzer.parser.util.Stores

enum class PayloadType { JSON, HTML, PDF_BASE64 }

data class RawReceipt(
    val store: Stores,
    val externalId: String,
    val sourceUrl: String,
    val type: PayloadType,
    val payload: String
) {
    fun bytes(): ByteArray = when (type) {
        PayloadType.PDF_BASE64 -> Base64.decode(payload)
        else -> payload.toByteArray(Charsets.UTF_8)
    }
}

interface StoreImporter {
    val store: Stores
    val startUrl: String
    val allowedHosts: Set<String>
    val loginHosts: Set<String> get() = emptySet()
    val scriptAsset: String
    val userAgent: String? get() = null

    fun shouldInject(url: String): Boolean

    fun isHostAllowed(host: String?): Boolean = matches(host, allowedHosts)

    fun isNavigationAllowed(host: String?): Boolean =
        matches(host, allowedHosts) || matches(host, loginHosts)

    private fun matches(host: String?, set: Set<String>) =
        host != null && set.any { host == it || host.endsWith(".$it") }
}