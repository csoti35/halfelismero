package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FishSpecies
import com.example.ui.components.FishCard
import com.example.ui.components.SpeciesDetailDialog
import com.example.ui.theme.TurquoisePrimary
import com.example.viewmodel.AquariumViewModel

@Composable
fun DatabaseScreen(
    viewModel: AquariumViewModel,
    modifier: Modifier = Modifier
) {
    val speciesList by viewModel.filteredSpecies.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFamilyFilter by viewModel.selectedFamilyFilter.collectAsStateWithLifecycle()
    val selectedSpeciesForDetail by viewModel.selectedSpeciesForDetail.collectAsStateWithLifecycle()
    val aquariums by viewModel.aquariums.collectAsStateWithLifecycle()

    var showAddCustomDialog by remember { mutableStateOf(false) }

    val families = listOf(
        "Összes",
        "Characidae",
        "Poeciliidae",
        "Cichlidae",
        "Osphronemidae",
        "Callichthyidae",
        "Cyprinidae",
        "Loricariidae"
    )

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("database_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomDialog = true },
                containerColor = TurquoisePrimary,
                contentColor = Color(0xFF002220),
                modifier = Modifier.testTag("add_custom_species_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Új faj hozzáadása")
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

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Keresés magyar vagy latin név szerint...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Keresés",
                        tint = TurquoisePrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Törlés")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("species_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Family Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                families.forEach { family ->
                    val isSelected = (selectedFamilyFilter == null && family == "Összes") ||
                            (selectedFamilyFilter == family)
                    InputChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectFamilyFilter(if (family == "Összes") null else family)
                        },
                        label = { Text(family) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = TurquoisePrimary.copy(alpha = 0.25f),
                            selectedLabelColor = TurquoisePrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${speciesList.size} halfaj találat (Offline elérhető)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Species List
            if (speciesList.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nincs találat erre a keresésre",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Próbálj más keresőszót, vagy add hozzá az új fajt!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(speciesList, key = { it.id }) { species ->
                        FishCard(
                            species = species,
                            onClick = { viewModel.selectSpeciesForDetail(species) }
                        )
                    }
                }
            }
        }
    }

    // Species Detail Dialog
    selectedSpeciesForDetail?.let { species ->
        SpeciesDetailDialog(
            species = species,
            onDismiss = { viewModel.selectSpeciesForDetail(null) },
            onAddToAquarium = {
                val defaultTank = aquariums.firstOrNull()
                if (defaultTank != null) {
                    viewModel.addFishToTank(defaultTank.id, species.hungarianName, species.scientificName, 1, "")
                }
            }
        )
    }

    // Add Custom Species Dialog
    if (showAddCustomDialog) {
        AddCustomSpeciesDialog(
            onDismiss = { showAddCustomDialog = false },
            onSave = { newSpecies ->
                // Save custom species
                viewModel.selectSpeciesForDetail(newSpecies)
                showAddCustomDialog = false
            }
        )
    }
}

@Composable
fun AddCustomSpeciesDialog(
    onDismiss: () -> Unit,
    onSave: (FishSpecies) -> Unit
) {
    var hungarianName by remember { mutableStateOf("") }
    var scientificName by remember { mutableStateOf("") }
    var family by remember { mutableStateOf("") }
    var sizeCm by remember { mutableStateOf("") }
    var minTemp by remember { mutableStateOf("24") }
    var maxTemp by remember { mutableStateOf("28") }
    var minPh by remember { mutableStateOf("6.5") }
    var maxPh by remember { mutableStateOf("7.5") }
    var minGh by remember { mutableStateOf("5") }
    var maxGh by remember { mutableStateOf("15") }
    var diet by remember { mutableStateOf("") }
    var temperament by remember { mutableStateOf("") }
    var breeding by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új halfaj hozzáadása az adatbázishoz") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = hungarianName,
                    onValueChange = { hungarianName = it },
                    label = { Text("Magyar név *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = scientificName,
                    onValueChange = { scientificName = it },
                    label = { Text("Tudományos név (latin) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = family,
                    onValueChange = { family = it },
                    label = { Text("Család") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = sizeCm,
                    onValueChange = { sizeCm = it },
                    label = { Text("Méret (pl. 5 cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hungarianName.isNotBlank()) {
                        val species = FishSpecies(
                            hungarianName = hungarianName,
                            scientificName = scientificName,
                            family = family.ifBlank { "Akváriumi halfélék" },
                            sizeCm = sizeCm.ifBlank { "5 cm" },
                            originHabitat = "Tenyésztett akváriumi változat",
                            minTempC = minTemp.toIntOrNull() ?: 24,
                            maxTempC = maxTemp.toIntOrNull() ?: 28,
                            minPh = minPh.toDoubleOrNull() ?: 6.5,
                            maxPh = maxPh.toDoubleOrNull() ?: 7.5,
                            minGh = minGh.toIntOrNull() ?: 5,
                            maxGh = maxGh.toIntOrNull() ?: 15,
                            diet = diet.ifBlank { "Mindenevő száraz és élő eleség" },
                            temperament = temperament.ifBlank { "Békés társas hal" },
                            breedingInfo = breeding.ifBlank { "Akváriumban szaporítható" },
                            description = "A felhasználó által manuálisan rögzített faj.",
                            careTips = "Rendszeres heti részleges vízcsere és szűrés ajánlott.",
                            isPreloaded = false
                        )
                        onSave(species)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                enabled = hungarianName.isNotBlank()
            ) {
                Text("Mentés", color = Color(0xFF002220))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )
}
