package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Aquarium
import com.example.data.model.AquariumFish
import com.example.ui.components.WaterParamColumn
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.TurquoisePrimary
import com.example.viewmodel.AquariumViewModel

@Composable
fun MyAquariumsScreen(
    viewModel: AquariumViewModel,
    modifier: Modifier = Modifier
) {
    val aquariums by viewModel.aquariums.collectAsStateWithLifecycle()
    val selectedAquarium by viewModel.selectedAquarium.collectAsStateWithLifecycle()
    val fishInSelectedTank by viewModel.currentTankFish.collectAsStateWithLifecycle()
    val allSpecies by viewModel.filteredSpecies.collectAsStateWithLifecycle()

    var showAddTankDialog by remember { mutableStateOf(false) }
    var showAddFishDialog by remember { mutableStateOf(false) }

    // Auto-select first tank if none is selected and list is not empty
    val activeTank = selectedAquarium ?: aquariums.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("my_aquariums_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTankDialog = true },
                containerColor = TurquoisePrimary,
                contentColor = Color(0xFF002220),
                modifier = Modifier.testTag("add_aquarium_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Új akvárium")
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

            // Tank selector row if multiple tanks exist
            if (aquariums.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aquariums.forEach { tank ->
                        val isSelected = (activeTank?.id == tank.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) TurquoisePrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectAquarium(tank) }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tank.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TurquoisePrimary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${tank.volumeLiters} Liter",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (activeTank == null) {
                // Empty State
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
                            imageVector = Icons.Default.Water,
                            contentDescription = null,
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Még nem rögzítettél akváriumot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Koppints a jobb alsó + gombra egy új medence felvételéhez!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddTankDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                        ) {
                            Text("Első akvárium hozzáadása", color = Color(0xFF002220))
                        }
                    }
                }
            } else {
                // Active Aquarium Detail & Fish Stock View
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Tank Overview Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activeTank.name,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Térfogat: ${activeTank.volumeLiters} Liter vízkapacitás",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TurquoisePrimary
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteAquarium(activeTank) },
                                        modifier = Modifier.testTag("delete_aquarium_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Akvárium törlése",
                                            tint = CoralAccent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Current Water Parameters
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
                                            icon = Icons.Default.Thermostat,
                                            label = "Hőmérséklet",
                                            value = "${activeTank.currentTempC} °C"
                                        )
                                        WaterParamColumn(
                                            icon = Icons.Default.WaterDrop,
                                            label = "pH érték",
                                            value = "${activeTank.currentPh}"
                                        )
                                        WaterParamColumn(
                                            icon = Icons.Default.Info,
                                            label = "Keménység",
                                            value = "${activeTank.currentGh} dGH"
                                        )
                                        WaterParamColumn(
                                            icon = Icons.Default.Info,
                                            label = "Nitrát (NO3)",
                                            value = "${activeTank.currentNo3} mg/l"
                                        )
                                    }
                                }

                                if (activeTank.filterType.isNotBlank() || activeTank.substrate.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Felszerelés & Talaj:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${activeTank.filterType} • ${activeTank.substrate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (activeTank.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = activeTank.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Fish Stock Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Akvárium halállománya",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Button(
                                onClick = { showAddFishDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TurquoisePrimary,
                                    contentColor = Color(0xFF002220)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_fish_to_tank_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hal hozzáadása", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Fish Stock Items
                    if (fishInSelectedTank.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pets,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ebben az akváriumban még nincsenek rögzített halak.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(fishInSelectedTank, key = { it.id }) { fish ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = TurquoisePrimary.copy(alpha = 0.2f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${fish.count}x",
                                                    fontWeight = FontWeight.Bold,
                                                    color = TurquoisePrimary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = fish.speciesName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (fish.scientificName.isNotBlank()) {
                                                Text(
                                                    text = fish.scientificName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontStyle = FontStyle.Italic,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (fish.notes.isNotBlank()) {
                                                Text(
                                                    text = fish.notes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeFishFromTank(fish.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hal törlése",
                                            tint = CoralAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Aquarium Dialog
    if (showAddTankDialog) {
        AddAquariumDialog(
            onDismiss = { showAddTankDialog = false },
            onConfirm = { name, liters, temp, ph, gh, no3, notes ->
                viewModel.addAquarium(name, liters, temp, ph, gh, no3, notes)
                showAddTankDialog = false
            }
        )
    }

    // Add Fish Dialog
    if (showAddFishDialog && activeTank != null) {
        AddFishDialog(
            speciesCatalog = allSpecies.map { it.hungarianName },
            onDismiss = { showAddFishDialog = false },
            onConfirm = { name, count, notes ->
                viewModel.addFishToTank(activeTank.id, name, "", count, notes)
                showAddFishDialog = false
            }
        )
    }
}

@Composable
fun AddAquariumDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, liters: Int, temp: Double, ph: Double, gh: Int, no3: Int, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var litersText by remember { mutableStateOf("120") }
    var tempText by remember { mutableStateOf("25.0") }
    var phText by remember { mutableStateOf("7.0") }
    var ghText by remember { mutableStateOf("10") }
    var no3Text by remember { mutableStateOf("15") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új akvárium regisztrálása") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Akvárium neve (pl. Nappali akvárium) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = litersText,
                        onValueChange = { litersText = it },
                        label = { Text("Liter *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        label = { Text("Hőfok (°C)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phText,
                        onValueChange = { phText = it },
                        label = { Text("pH") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ghText,
                        onValueChange = { ghText = it },
                        label = { Text("dGH") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = no3Text,
                        onValueChange = { no3Text = it },
                        label = { Text("NO3") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Megjegyzések (szűrő, növények)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val liters = litersText.toIntOrNull() ?: 50
                    val temp = tempText.toDoubleOrNull() ?: 25.0
                    val ph = phText.toDoubleOrNull() ?: 7.0
                    val gh = ghText.toIntOrNull() ?: 10
                    val no3 = no3Text.toIntOrNull() ?: 15
                    if (name.isNotBlank()) {
                        onConfirm(name, liters, temp, ph, gh, no3, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                enabled = name.isNotBlank()
            ) {
                Text("Létrehozás", color = Color(0xFF002220))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )
}

@Composable
fun AddFishDialog(
    speciesCatalog: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (speciesName: String, count: Int, notes: String) -> Unit
) {
    var speciesName by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hal hozzáadása az akváriumhoz") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = speciesName,
                    onValueChange = { speciesName = it },
                    label = { Text("Faj neve (pl. Neonhal) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text("Darabszám") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Megjegyzés (pl. hím, fiatal ivadék)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = countText.toIntOrNull() ?: 1
                    if (speciesName.isNotBlank()) {
                        onConfirm(speciesName, count, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                enabled = speciesName.isNotBlank()
            ) {
                Text("Hozzáadás", color = Color(0xFF002220))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )
}
