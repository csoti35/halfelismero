package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Aquarium
import com.example.ui.components.FishCard
import com.example.ui.components.SpeciesDetailDialog
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.GoldenAmber
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AquariumViewModel

@Composable
fun HomeScreen(
    viewModel: AquariumViewModel,
    modifier: Modifier = Modifier
) {
    val aquariums by viewModel.aquariums.collectAsStateWithLifecycle()
    val allSpecies by viewModel.filteredSpecies.collectAsStateWithLifecycle()
    val careLogs by viewModel.careLogs.collectAsStateWithLifecycle()
    val selectedSpeciesForDetail by viewModel.selectedSpeciesForDetail.collectAsStateWithLifecycle()

    val featuredFish = allSpecies.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .testTag("home_screen")
    ) {
        // Hero Aquascape Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.aquarium_hero),
                contentDescription = "Akvárium háttérkép",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x66071526),
                                Color(0xE6071526)
                            )
                        )
                    )
            )

            // Hero content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TurquoiseDark.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mesterséges Intelligencia & Enciklopédia",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Akvárium Halfelismerő",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Azonnali halfaj azonosítás és professzionális akváriumkezelés",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD0E6F5)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Grid
        Text(
            text = "Gyorsműveletek",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.CameraAlt,
                title = "Halfelismerés",
                subtitle = "Fotó alapján AI",
                accentColor = TurquoisePrimary,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(AppScreen.IDENTIFY) },
                tag = "quick_action_identify"
            )

            QuickActionCard(
                icon = Icons.Default.MenuBook,
                title = "Fajadatbázis",
                subtitle = "Offline enciklopédia",
                accentColor = Color(0xFF4DD0E1),
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(AppScreen.DATABASE) },
                tag = "quick_action_database"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.Water,
                title = "Akváriumaim",
                subtitle = "${aquariums.size} regisztrált medence",
                accentColor = Color(0xFF64B5F6),
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(AppScreen.MY_AQUARIUMS) },
                tag = "quick_action_aquariums"
            )

            QuickActionCard(
                icon = Icons.Default.EventNote,
                title = "Gondozási napló",
                subtitle = "Vízcsere & etetés",
                accentColor = GoldenAmber,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(AppScreen.CARE_LOG) },
                tag = "quick_action_care_log"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Spotlight Fish of the Day
        if (featuredFish != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kiemelt halfaj",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Összes megtekintése",
                    style = MaterialTheme.typography.labelMedium,
                    color = TurquoisePrimary,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.DATABASE) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                FishCard(
                    species = featuredFish,
                    onClick = { viewModel.selectSpeciesForDetail(featuredFish) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Aquariums Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Akváriumaim állapota",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Kezelés",
                style = MaterialTheme.typography.labelMedium,
                color = TurquoisePrimary,
                modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.MY_AQUARIUMS) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (aquariums.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Még nem adtál hozzá akváriumot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.MY_AQUARIUMS) },
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                    ) {
                        Text("Akvárium felvétele", color = Color(0xFF002220))
                    }
                }
            }
        } else {
            aquariums.take(2).forEach { tank ->
                AquariumSummaryCard(
                    aquarium = tank,
                    onClick = {
                        viewModel.selectAquarium(tank)
                        viewModel.navigateTo(AppScreen.MY_AQUARIUMS)
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Care Activities
        if (careLogs.isNotEmpty()) {
            Text(
                text = "Legutóbbi gondozási napló",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            careLogs.take(2).forEach { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (log.type) {
                                "WATER_CHANGE" -> TurquoisePrimary.copy(alpha = 0.2f)
                                "WATER_TEST" -> GoldenAmber.copy(alpha = 0.2f)
                                else -> Color(0xFF4DD0E1).copy(alpha = 0.2f)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (log.type) {
                                        "WATER_CHANGE" -> Icons.Default.WaterDrop
                                        "WATER_TEST" -> Icons.Default.CheckCircle
                                        else -> Icons.Default.EventNote
                                    },
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${log.aquariumName} • ${log.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
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
                val firstTank = aquariums.firstOrNull()
                if (firstTank != null) {
                    viewModel.addFishToTank(firstTank.id, species.hungarianName, species.scientificName, 1, "")
                }
            }
        )
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun AquariumSummaryCard(
    aquarium: Aquarium,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("aquarium_card_${aquarium.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = aquarium.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TurquoisePrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${aquarium.volumeLiters} L",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TurquoisePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${aquarium.currentTempC} °C • pH ${aquarium.currentPh} • ${aquarium.currentGh} dGH • NO3: ${aquarium.currentNo3} mg/l",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Megnyitás",
                tint = TurquoisePrimary
            )
        }
    }
}
