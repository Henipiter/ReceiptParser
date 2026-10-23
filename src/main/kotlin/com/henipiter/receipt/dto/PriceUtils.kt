package com.henipiter.receipt.dto

import kotlin.math.roundToInt

class PriceUtils {

    companion object {

        fun intPriceToString(integer: Int): String {
            return doubleToString(integer / 100.0)
        }

        fun intValueToString(integer: Int): String {
            return intToString(integer)
        }

        fun intQuantityToString(integer: Int): String {
            return quantityToString(integer / 1000.0)
        }

        fun doublePriceTextToInt(text: CharSequence?): Int {
            if (text == null) {
                return 0
            }
            return doublePriceTextToInt(text.toString())
        }

        fun doublePriceTextToInt(text: String): Int {
            if (text == "null" || text == "") {
                return 0
            }
            val newText = fixPriceStartingWithComma(text)
            return (newText.toDouble() * 100).roundToInt()
        }

        fun doubleQuantityTextToInt(text: CharSequence?): Int {
            if (text == null) {
                return 0
            }
            return doubleQuantityTextToInt(text.toString())
        }

        fun doubleQuantityTextToInt(text: String): Int {
            if (text == "null" || text == "") {
                return 0
            }
            val newText = fixPriceStartingWithComma(text)
            return (newText.toDouble() * 1000).roundToInt()
        }

        fun roundInt(double: Double, multiplier: Int): Int {
            return (double * multiplier).toInt()
        }

        private fun fixPriceStartingWithComma(price: String): String {
            if (price.isNotEmpty() && (price[0] == '.' || price[0] == ',')) {
                return "0$price"
            }
            return price
        }

        private fun intToString(integer: Int?): String {
            val double = (integer ?: 1) / 100.0

            val format = when {
                double < 0.1 -> "%.2f"
                double < 1 -> "%.1f"
                else -> "%.0f"
            }
            return format.format(double)
        }

        private fun doubleToString(value: Double?): String {
            val safeValue = value?.takeIf { it.isFinite() } ?: 1.0
            return "%.2f".format(safeValue)
        }

        private fun quantityToString(value: Double): String {
            val safeValue = value.takeIf { it.isFinite() } ?: 1.0

            return if (safeValue % 1.0 == 0.0) {
                "%.0f".format(safeValue)
            } else {
                "%.3f".format(safeValue)
            }
        }
    }
}
