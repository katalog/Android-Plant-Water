package com.moonkata.plantwater.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

// 카메라 원본 해상도 그대로 디코딩하면 메모리를 많이 먹으니 표시/전송 용도에 맞게 다운샘플링해서 읽음
object PhotoUtils {

    fun decodeBitmap(photoUri: String?, maxDimension: Int = 1024): Bitmap? {
        if (photoUri.isNullOrBlank()) return null
        val path = Uri.parse(photoUri).path ?: return null

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, maxDimension)
        }
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
        var inSampleSize = 1
        while (options.outHeight / inSampleSize > maxDimension || options.outWidth / inSampleSize > maxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
