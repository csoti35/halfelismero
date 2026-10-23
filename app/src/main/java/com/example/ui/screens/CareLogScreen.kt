package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CareLogEntry
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.GoldenAmber
import com.example.ui.theme.TurquoisePrimary
import com.example.viewmodel.AquariumViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CareLogScreen(
    viewModel: AquariumViewModel,
    modifier: Modifier = Modifier
) {
    val careLogs by viewModel.careLogs.collectAsStateWithLifecycle()
    val logFilter by viewModel.logFilter.collectAsStateWithLifecycle()
    val aquariums by viewModel.aquariums.collectAsStateWithLifecycle()

    var showAddLogDialog by remember { mutableStateOf(false) }

    val filteredLogs = careLogs.filter { log ->
        when (logFilter) {
            "ALL" -> true
            else -> log.type == logFilter
        }
    }

    val filterOptions = listOf(
        "ALL" to "Összes bejegyzés",
        "WATER_CHANGE" to "Vízcsere",
        "FEEDING" to "Etetés",
        "WATER_TEST" to "Vízteszt",
        "BREEDING" to "Szaporítás"
    )

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("care_log_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddLogDialog = true },
                containerColor = TurquoisePrimary,
                contentColor = Color(0xFF002220),
                modifier = Modifier.testTag("add_care_log_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Új gondozási bejegyzés")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { (key, label) ->
                    val isSelected = (logFilter == key)
                    InputChip(
                        selected = isSelected,
                        onClick = { viewModel.setLogFilter(key) },
                        label = { Text(label) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = TurquoisePrimary.copy(alpha = 0.25f),
                            selectedLabelColor = TurquoisePrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredLogs.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nincs még bejegyzés ebben a kategóriában",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Rögzíts vízcserét, etetést, víztesztet vagy szaporítási eseményt a + gombbal!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddLogDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                        ) {
                            Text("Új gondozási esemény", color = Color(0xFF002220))
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        CareLogItemCard(
                            log = log,
                            onDelete = { viewModel.deleteCareLog(log) }
                        )
                    }
                }
            }
        }
    }

    // Add Care Log Dialog
    if (showAddLogDialog) {
        AddCareLogDialog(
            aquariums = aquariums.map { it.id to it.name },
            onDismiss = { showAddLogDialog = false },
            onConfirm = { tankId, tankName, type, title, waterAmount, temp, ph, gh, no3, notes ->
                viewModel.addCareLog(
                    aquariumId = tankId,
                    aquariumName = tankName,
                    type = type,
                    title = title,
                    waterAmount = waterAmount,
                    temp = temp,
                    ph = ph,
                    gh = gh,
                    no3 = no3,
                    notes = notes
                )
                showAddLogDialog = false
            }
        )
    }
}

@Composable
fun CareLogItemCard(
    log: CareLogEntry,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy. MM. dd. HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(log.timestamp))

    val (icon, color, typeLabel) = when (log.type) {
        "WATER_CHANGE" -> Triple(Icons.Default.WaterDrop, TurquoisePrimary, "Vízcsere")
        "FEEDING" -> Triple(Icons.Default.Restaurant, GoldenAmber, "Etetés")
        "WATER_TEST" -> Triple(Icons.Default.CheckCircle, Color(0xFF4DD0E1), "Vízteszt")
        "BREEDING" -> Triple(Icons.Default.Egg, CoralAccent, "Szaporítás")
        else -> Triple(Icons.Default.EventNote, TurquoisePrimary, "Karbantartás")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${log.aquariumName} • $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                if (log.waterAmountLiters > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kicserélt vízmennyiség: ${log.waterAmountLiters} liter",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TurquoisePrimary
                    )
                }

                if (log.measuredTemp != null || log.measuredPh != null || log.measuredGh != null || log.measuredNo3 != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val paramsText = listOfNotNull(
                        log.measuredTemp?.let { "${it} °C" },
                        log.measuredPh?.let { "pH $it" },
                        log.measuredGh?.let { "$it dGH" },
                        log.measuredNo3?.let { "NO3: $it mg/l" }
                    ).joinToString(" • ")

                    Text(
                        text = "Mért paraméterek: $paramsText",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4DD0E1)
                    )
                }

                if (log.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Törlés",
                    tint = CoralAccent.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddCareLogDialog(
    aquariums: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onConfirm: (
        tankId: Long,
        tankName: String,
        type: String,
        title: String,
        waterAmount: Int,
        temp: Double?,
        ph: Double?,
        gh: Int?,
        no3: Int?,
        notes: String
    ) -> Unit
) {
    var selectedTankId by remember { mutableStateOf(aquariums.firstOrNull()?.first ?: 1L) }
    var selectedTankName by remember { mutableStateOf(aquariums.firstOrNull()?.second ?: "Fő akvárium") }
    var selectedType by remember { mutableStateOf("WATER_CHANGE") }
    var title by remember { mutableStateOf("30%-os részleges vízcsere") }
    var waterAmountText by remember { mutableStateOf("35") }
    var tempText by remember { mutableStateOf("25.0") }
    var phText by remember { mutableStateOf("6.8") }
    var ghText by remember { mutableStateOf("8") }
    var no3Text by remember { mutableStateOf("10") }
    var notes by remember { mutableStateOf("") }

    val types = listOf(
        "WATER_CHANGE" to "Vízcsere",
        "FEEDING" to "Etetés",
        "WATER_TEST" to "Vízteszt",
        "BREEDING" to "Szaporítás"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gondozási napló bejegyzés") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Type selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { (typeKey, typeLabel) ->
                        val isSelected = selectedType == typeKey
                        InputChip(
                            selected = isSelected,
                            onClick = {
                                selectedType = typeKey
                                title = when (typeKey) {
                                    "WATER_CHANGE" -> "30%-os heti vízcsere"
                                    "FEEDING" -> "Fagyasztott artemia etetés"
                                    "WATER_TEST" -> "Heti cseppteszt ellenőrzés"
                                    "BREEDING" -> "Új ikrázás / kishal megfigyelés"
                                    else -> "Karbantartás"
                                }
                            },
                            label = { Text(typeLabel) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Esemény megnevezése *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == "WATER_CHANGE") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = waterAmountText,
                        onValueChange = { waterAmountText = it },
                        label = { Text("Kicserélt víz (liter)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedType == "WATER_TEST") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = tempText,
                            onValueChange = { tempText = it },
                            label = { Text("Hőfok") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phText,
                            onValueChange = { phText = it },
                            label = { Text("pH") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = ghText,
                            onValueChange = { ghText = it },
                            label = { Text("dGH") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = no3Text,
                            onValueChange = { no3Text = it },
                            label = { Text("NO3") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Részletes jegyzet, megfigyelések") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val waterAmount = waterAmountText.toIntOrNull() ?: 0
                        val temp = tempText.toDoubleOrNull()
                        val ph = phText.toDoubleOrNull()
                        val gh = ghText.toIntOrNull()
                        val no3 = no3Text.toIntOrNull()

                        onConfirm(
                            selectedTankId,
                            selectedTankName,
                            selectedType,
                            title,
                            waterAmount,
                            temp,
                            ph,
                            gh,
                            no3,
                            notes
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                enabled = title.isNotBlank()
            ) {
                Text("Rögzítés", color = Color(0xFF002220))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )
}
