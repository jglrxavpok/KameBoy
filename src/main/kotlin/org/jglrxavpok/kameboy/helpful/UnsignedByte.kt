package org.jglrxavpok.kameboy.helpful

fun Byte.asUnsigned() = (this.toInt()) and 0xFF
fun Int.asUnsigned16() = this and 0xFFFF
fun Int.asUnsigned8() = this and 0xFF
fun Int.asAddress() = asUnsigned16()
fun Int.asSigned8(): Int {
    val value = this and 0b01111111
    val sign = this and 0b10000000
    return value + (-(1 shl 15))*sign
}

fun Byte.asAddress() = asUnsigned().asAddress()

fun fromNibbles(high: Byte, low: Byte) = high.asUnsigned() shl 8 or low.asUnsigned()

fun AsciiString(data: ByteArray): String {
    val end = data.indexOfFirst { it == 0.toByte() }
    return if(end == -1)
        String(data, Charsets.US_ASCII)
    else
        String(data, 0, end, Charsets.US_ASCII)
}

fun Int.setBits(bitfield: Int, location: IntRange): Int {
    val start = location.start
    var result = this
    for(index in location) {
        val bit = bitfield and (1 shl (index-start))
        val mask = (1 shl index)
        val currentBitSet = result and mask
        when {
            currentBitSet != 0 && bit != 0 -> Unit // already correct
            currentBitSet == 0 && bit == 0 -> Unit
            currentBitSet != 0 && bit == 0 -> { result = result and (mask).inv() } // unset
            currentBitSet == 0 && bit != 0 -> { result = result or mask } // set
        }
    }
    return result
}