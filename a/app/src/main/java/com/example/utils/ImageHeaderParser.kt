package com.example.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImageHeaderParser {

    data class ImageDimensions(val width: Int, val height: Int)

    fun parseImageDimensionsFromBuffer(buf: ByteArray): ImageDimensions? {
        if (buf.size < 24) return null

        // 1. PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((buf[0].toInt() and 0xFF) == 0x89 &&
            (buf[1].toInt() and 0xFF) == 0x50 &&
            (buf[2].toInt() and 0xFF) == 0x4E &&
            (buf[3].toInt() and 0xFF) == 0x47
        ) {
            val bb = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN)
            val width = bb.getInt(16)
            val height = bb.getInt(20)
            if (width > 0 && height > 0) return ImageDimensions(width, height)
        }

        // 2. GIF: "GIF8"
        if ((buf[0].toInt() and 0xFF) == 0x47 &&
            (buf[1].toInt() and 0xFF) == 0x49 &&
            (buf[2].toInt() and 0xFF) == 0x46 &&
            (buf[3].toInt() and 0xFF) == 0x38
        ) {
            val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
            val width = bb.getShort(6).toInt() and 0xFFFF
            val height = bb.getShort(8).toInt() and 0xFFFF
            if (width > 0 && height > 0) return ImageDimensions(width, height)
        }

        // 3. WEBP: "RIFF" .... "WEBP"
        if ((buf[0].toInt() and 0xFF) == 0x52 &&
            (buf[1].toInt() and 0xFF) == 0x49 &&
            (buf[2].toInt() and 0xFF) == 0x46 &&
            (buf[3].toInt() and 0xFF) == 0x46 &&
            (buf[8].toInt() and 0xFF) == 0x57 &&
            (buf[9].toInt() and 0xFF) == 0x45 &&
            (buf[10].toInt() and 0xFF) == 0x42 &&
            (buf[11].toInt() and 0xFF) == 0x50
        ) {
            val chunkHeader = String(buf, 12, 4, Charsets.US_ASCII)
            if (chunkHeader == "VP8X" && buf.size >= 30) {
                val width = 1 + ((buf[24].toInt() and 0xFF) or
                        ((buf[25].toInt() and 0xFF) shl 8) or
                        ((buf[26].toInt() and 0xFF) shl 16))
                val height = 1 + ((buf[27].toInt() and 0xFF) or
                        ((buf[28].toInt() and 0xFF) shl 8) or
                        ((buf[29].toInt() and 0xFF) shl 16))
                return ImageDimensions(width, height)
            }
            if (chunkHeader == "VP8 " && buf.size >= 30) {
                val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
                val width = bb.getShort(26).toInt() and 0x3FFF
                val height = bb.getShort(28).toInt() and 0x3FFF
                if (width > 0 && height > 0) return ImageDimensions(width, height)
            }
            if (chunkHeader == "VP8L" && buf.size >= 25) {
                val b0 = buf[21].toInt() and 0xFF
                val b1 = buf[22].toInt() and 0xFF
                val b2 = buf[23].toInt() and 0xFF
                val b3 = buf[24].toInt() and 0xFF
                val width = 1 + (((b1 and 0x3F) shl 8) or b0)
                val height = 1 + (((b3 and 0x0F) shl 10) or (b2 shl 2) or ((b1 and 0xC0) shr 6))
                if (width > 0 && height > 0) return ImageDimensions(width, height)
            }
        }

        // 4. JPEG: FF D8
        if ((buf[0].toInt() and 0xFF) == 0xFF && (buf[1].toInt() and 0xFF) == 0xD8) {
            var offset = 2
            val bb = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN)
            while (offset + 9 < buf.size) {
                if ((buf[offset].toInt() and 0xFF) != 0xFF) {
                    offset += 1
                    continue
                }
                val marker = buf[offset + 1].toInt() and 0xFF
                if (marker == 0xFF) {
                    offset += 1
                    continue
                }
                if (marker == 0xC0 || marker == 0xC1 || marker == 0xC2 || marker == 0xC3) {
                    val height = bb.getShort(offset + 5).toInt() and 0xFFFF
                    val width = bb.getShort(offset + 7).toInt() and 0xFFFF
                    if (width > 0 && height > 0) return ImageDimensions(width, height)
                    break
                }
                if (marker == 0xD9 || marker == 0xDA) break
                val segmentLength = bb.getShort(offset + 2).toInt() and 0xFFFF
                if (segmentLength < 2) break
                offset += 2 + segmentLength
            }
        }

        return null
    }
}
