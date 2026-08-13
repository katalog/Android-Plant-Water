package com.moonkata.plantwater.ui.recognition

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moonkata.plantwater.recognition.GeminiPlantIdentifier
import com.moonkata.plantwater.recognition.PlantIdentification
import com.moonkata.plantwater.util.PhotoUtils

@Composable
fun RecognitionScreen(
    photoUri: Uri,
    onResult: (PlantIdentification?) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoUri) { PhotoUtils.decodeBitmap(photoUri.toString()) }

    LaunchedEffect(photoUri) {
        val result = bitmap?.let { GeminiPlantIdentifier.identify(it).getOrNull() }
        onResult(result)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )
            }
            CircularProgressIndicator()
            Text(
                text = "식물을 확인하고 있어요...",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
