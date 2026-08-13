package com.moonkata.plantwater.ui.info

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moonkata.plantwater.recognition.PlantCatalog
import com.moonkata.plantwater.recognition.PlantCatalogEntry
import com.moonkata.plantwater.recognition.PlantIdentification
import com.moonkata.plantwater.ui.theme.PlantWaterTheme
import com.moonkata.plantwater.util.PhotoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    photoUri: Uri?,
    identification: PlantIdentification?,
    onConfirm: (name: String, species: String, intervalDays: Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isManualMode by remember(identification) { mutableStateOf(identification == null) }
    var name by remember(identification) { mutableStateOf(identification?.commonName ?: "") }
    var selectedEntry by remember { mutableStateOf<PlantCatalogEntry?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val species = if (isManualMode) selectedEntry?.scientificName else identification?.scientificName
    val intervalDays = if (isManualMode) selectedEntry?.wateringIntervalDays ?: 7 else identification?.wateringIntervalDays ?: 7
    val wateringAdvice = if (isManualMode) {
        selectedEntry?.let { "${it.wateringIntervalDays}일마다 물주기 권장" } ?: "종을 선택해주세요"
    } else {
        identification?.wateringAdvice ?: ""
    }
    val lightAdvice = if (isManualMode) selectedEntry?.lightAdvice ?: "" else identification?.lightAdvice ?: ""

    val canConfirm = name.isNotBlank() && (!isManualMode || selectedEntry != null)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "정보 확인") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "닫기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PhotoThumbnail(photoUri = photoUri)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = "식물 이름") },
                modifier = Modifier.fillMaxWidth()
            )

            if (!isManualMode) {
                Text(text = species.orEmpty(), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = {
                    isManualMode = true
                }) {
                    Text(text = "인식이 틀렸나요? 직접 선택하기")
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedEntry?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = "종 선택") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        PlantCatalog.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(text = entry.name) },
                                onClick = {
                                    selectedEntry = entry
                                    if (name.isBlank()) name = entry.name
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (wateringAdvice.isNotBlank() || lightAdvice.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard(title = "물주기", body = wateringAdvice)
                    InfoCard(title = "광량", body = lightAdvice)
                }
            }

            Button(
                onClick = { onConfirm(name.trim(), species.orEmpty(), intervalDays) },
                enabled = canConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "알림 등록하기")
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(photoUri: Uri?, modifier: Modifier = Modifier) {
    val bitmap = remember(photoUri) { PhotoUtils.decodeBitmap(photoUri?.toString()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = "🌱", style = MaterialTheme.typography.displayMedium)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoScreenRecognizedPreview() {
    PlantWaterTheme {
        InfoScreen(
            photoUri = null,
            identification = PlantIdentification(
                commonName = "몬스테라",
                scientificName = "Monstera deliciosa",
                wateringIntervalDays = 7,
                wateringAdvice = "7일마다 흙이 마르면 물주기",
                lightAdvice = "밝은 간접광"
            ),
            onConfirm = { _, _, _ -> },
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoScreenManualPreview() {
    PlantWaterTheme {
        InfoScreen(
            photoUri = null,
            identification = null,
            onConfirm = { _, _, _ -> },
            onNavigateBack = {}
        )
    }
}
