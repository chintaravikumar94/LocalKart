package com.localkart.common.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/** Generates a QR bitmap for the given content (e.g. a seller profile deep link). */
fun generateQr(content: String, size: Int = 512): Bitmap {
    val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size) {
        bmp.setPixel(x, y, if (bits[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
    }
    return bmp
}

@Composable
fun QrImage(content: String, modifier: Modifier = Modifier) {
    val img: ImageBitmap = remember(content) { generateQr(content).asImageBitmap() }
    Image(bitmap = img, contentDescription = "QR code", modifier = modifier)
}
