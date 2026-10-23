package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.camera.CameraCaptureScreen
import com.example.ui.components.DetailRowItem
import com.example.ui.components.WaterParamColumn
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.GoldenAmber
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.viewmodel.AquariumViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdentifyScreen(
    viewModel: AquariumViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.recognitionState.collectAsStateWithLifecycle()
    val aquariums by viewModel.aquariums.collectAsStateWithLifecycle()

    var showCameraView by remember { mutableStateOf(false) }
    var showAddToTankDialog by remember { mutableStateOf(false) }
    var selectedTankForFish by remember { mutableStateOf(aquariums.firstOrNull()) }
    var fishCountToAdd by remember { mutableStateOf(1) }

    // If CameraX view is active, display full CameraX screen
    if (showCameraView) {
        CameraCaptureScreen(
            onPhotoCaptured = { bitmap ->
                viewModel.setImageBitmap(bitmap)
                showCameraView = false
            },
            onClose = { showCameraView = false }
        )
        return
    }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setImageFromUri(uri)
        }
    }

    // System Camera Launcher Fallback
    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.setImageBitmap(bitmap)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("identify_screen")
    ) {
        // Title section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = TurquoisePrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Akváriumi Halfelismerő",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Készíts fotót vagy válassz képet az AI elemzéshez",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Container / Placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 1.5.dp,
                    color = if (state.imageBitmap != null) TurquoisePrimary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(20.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (state.imageBitmap != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = state.imageBitmap!!.asImageBitmap(),
                        contentDescription = "Felismerendő hal fotója",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay buttons to retake/change
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = "Kép módosítása",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = TurquoisePrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Nincs még hal fotó kiválasztva",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Fotózd le a halat oldalról, jó megvilágításban a legpontosabb eredményért",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Capture & Pick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showCameraView = true },
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("take_photo_camerax_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TurquoisePrimary,
                    contentColor = Color(0xFF002220)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CameraX", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { systemCameraLauncher.launch(null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("take_photo_system_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kamera", fontSize = 13.sp)
            }

            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_gallery_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Galéria", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Analyze Button
        Button(
            onClick = { viewModel.startIdentification() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("analyze_fish_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = TurquoisePrimary,
                contentColor = Color(0xFF002220)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !state.isAnalyzing && state.imageBitmap != null
        ) {
            if (state.isAnalyzing) {
                CircularProgressIndicator(
                    color = Color(0xFF002220),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Hal elemzése AI modellel...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Halfelismerés indítása", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Demo fallback button if no key or for instant preview
        if (state.imageBitmap == null || state.isApiKeyMissing) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.runSampleDemoIdentification() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sample_demo_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Minta felismerés tesztelése (kép nélkül)", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Error Banner
        state.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CoralAccent.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CoralAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isApiKeyMissing) "API-kulcs beállítás szükséges" else "Hiba történt",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CoralAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (state.isApiKeyMissing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.runSampleDemoIdentification() },
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                        ) {
                            Text("Minta adatok megjelenítése", color = Color(0xFF002220), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Recognition Result Section
        state.result?.let { result ->
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("recognition_result_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header: Confidence and Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.confidencePercent >= 80) TurquoisePrimary.copy(alpha = 0.2f) else GoldenAmber.copy(alpha = 0.2f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (result.confidencePercent >= 80) TurquoisePrimary else GoldenAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${result.confidencePercent}% egyezés",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (result.confidencePercent >= 80) TurquoisePrimary else GoldenAmber
                                )
                            }
                        }

                        if (state.isSimulatedDemo) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldenAmber.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Minta bemutató",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldenAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = result.hungarianName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = result.scientificName,
                        style = MaterialTheme.typography.titleMedium,
                        fontStyle = FontStyle.Italic,
                        color = TurquoisePrimary
                    )
                    if (result.family.isNotBlank()) {
                        Text(
                            text = result.family,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bővebb leírás a diszhal.info oldalon (a felismert faj latin neve alapján)
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    val searchName = result.scientificName.ifBlank { result.hungarianName }
                    if (searchName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val q = java.net.URLEncoder.encode("site:diszhal.info " + searchName, "UTF-8")
                                try { uriHandler.openUri("https://www.google.com/search?q=" + q) } catch (e: Exception) { }
                            },
                            modifier = Modifier.testTag("diszhal_info_button")
                        ) {
                            Text("Bővebben a diszhal.info-n", color = TurquoisePrimary, fontSize = 13.sp)
                        }
                    }

                    // Uncertainty Warning banner if present
                    if (result.uncertaintyWarning.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GoldenAmber.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = GoldenAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Bizonytalansági tényező:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldenAmber
                                    )
                                    Text(
                                        text = result.uncertaintyWarning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Alternative Species Chips
                    if (result.alternativeSpecies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Hasonló fajok, amivel összetéveszthető (nem ez az eredmény):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            result.alternativeSpecies.forEach { altSpecies ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = altSpecies,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Water Parameters Table
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WaterParamColumn(
                                icon = Icons.Default.Info,
                                label = "Hőmérséklet",
                                value = result.temperature.ifBlank { "24-28 °C" }
                            )
                            WaterParamColumn(
                                icon = Icons.Default.Info,
                                label = "pH tartomány",
                                value = result.ph.ifBlank { "6.5-7.5" }
                            )
                            WaterParamColumn(
                                icon = Icons.Default.Info,
                                label = "Keménység",
                                value = result.hardness.ifBlank { "5-15 dGH" }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (result.description.isNotBlank()) {
                        DetailRowItem(
                            icon = Icons.Default.Info,
                            title = "Leírás",
                            content = result.description
                        )
                    }

                    if (result.diet.isNotBlank()) {
                        DetailRowItem(
                            icon = Icons.Default.Info,
                            title = "Táplálkozás",
                            content = result.diet
                        )
                    }

                    if (result.compatibility.isNotBlank()) {
                        DetailRowItem(
                            icon = Icons.Default.Info,
                            title = "Társíthatóság",
                            content = result.compatibility
                        )
                    }

                    if (result.careTips.isNotBlank()) {
                        DetailRowItem(
                            icon = Icons.Default.Lightbulb,
                            title = "Gondozási tanácsok",
                            content = result.careTips
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons: Add to tank & Save to database
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (aquariums.isNotEmpty()) {
                                    selectedTankForFish = aquariums.first()
                                    showAddToTankDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_identified_to_tank_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurquoisePrimary,
                                contentColor = Color(0xFF002220)
                            ),
                            enabled = aquariums.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Akváriumhoz", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.saveIdentifiedFishToDatabase() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_identified_to_db_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.hasSaved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surface,
                                contentColor = Color.White
                            ),
                            enabled = !state.hasSaved
                        ) {
                            Icon(
                                imageVector = if (state.hasSaved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (state.hasSaved) "Mentve" else "Faj mentése")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { viewModel.resetRecognition() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Új kép fotózása")
                    }
                }
            }
        }
    }

    // Add To Tank Dialog
    if (showAddToTankDialog && selectedTankForFish != null) {
        AlertDialog(
            onDismissRequest = { showAddToTankDialog = false },
            title = { Text("Hozzáadás akváriumhoz") },
            text = {
                Column {
                    Text(
                        text = "Válaszd ki az akváriumot, ahová a felismert halat szeretnéd rögzíteni:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    aquariums.forEach { tank ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedTankForFish?.id == tank.id) TurquoisePrimary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable { selectedTankForFish = tank }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "${tank.name} (${tank.volumeLiters} L)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTankForFish?.id == tank.id) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Darabszám:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { if (fishCountToAdd > 1) fishCountToAdd-- },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) { Text("-") }
                            Text(
                                text = " $fishCountToAdd db ",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Button(
                                onClick = { fishCountToAdd++ },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) { Text("+") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTankForFish?.let { tank ->
                            viewModel.addIdentifiedFishToAquarium(tank.id, fishCountToAdd)
                        }
                        showAddToTankDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    Text("Hozzáadás", color = Color(0xFF002220))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddToTankDialog = false }) {
                    Text("Mégse")
                }
            }
        )
    }
}
