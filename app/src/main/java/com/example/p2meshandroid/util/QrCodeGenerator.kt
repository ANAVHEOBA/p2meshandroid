package com.example.p2meshandroid.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Utility object for generating QR codes
 */
object QrCodeGenerator {

    /**
     * Generate a QR code bitmap from the given content
     *
     * @param content The string content to encode in the QR code
     * @param size The width and height of the QR code in pixels
     * @param foregroundColor The color of the QR code modules (default: black)
     * @param backgroundColor The background color (default: white)
     * @return A Bitmap containing the QR code, or null if generation fails
     */
    fun generateQrCode(
        content: String,
        size: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (bitMatrix[x, y]) foregroundColor else backgroundColor
                }
            }

            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, size, 0, 0, size, size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate a QR code as a Compose ImageBitmap
     *
     * @param content The string content to encode in the QR code
     * @param size The width and height of the QR code in pixels
     * @param foregroundColor The color of the QR code modules (default: black)
     * @param backgroundColor The background color (default: white)
     * @return An ImageBitmap containing the QR code, or null if generation fails
     */
    fun generateQrCodeImageBitmap(
        content: String,
        size: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): ImageBitmap? {
        return generateQrCode(content, size, foregroundColor, backgroundColor)?.asImageBitmap()
    }
}
