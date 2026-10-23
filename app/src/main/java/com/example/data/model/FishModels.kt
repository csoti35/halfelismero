package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fish_species")
data class FishSpecies(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hungarianName: String,
    val scientificName: String,
    val family: String,
    val sizeCm: String,
    val originHabitat: String,
    val minTempC: Int,
    val maxTempC: Int,
    val minPh: Double,
    val maxPh: Double,
    val minGh: Int,
    val maxGh: Int,
    val diet: String,
    val temperament: String,
    val breedingInfo: String,
    val description: String,
    val careTips: String,
    val minTankLiters: Int = 50,
    val isPreloaded: Boolean = true
)

@Entity(tableName = "aquariums")
data class Aquarium(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val volumeLiters: Int,
    val currentTempC: Double = 25.0,
    val currentPh: Double = 7.0,
    val currentGh: Int = 10,
    val currentNo3: Int = 15,
    val substrate: String = "Apró szemű bazalt kavics",
    val filterType: String = "Külső szűrő biológiai töltettel",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "aquarium_fish")
data class AquariumFish(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val aquariumId: Long,
    val speciesName: String,
    val scientificName: String = "",
    val count: Int = 1,
    val dateAdded: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "care_logs")
data class CareLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val aquariumId: Long,
    val aquariumName: String,
    val type: String, // "WATER_CHANGE", "FEEDING", "WATER_TEST", "BREEDING", "MAINTENANCE"
    val timestamp: Long = System.currentTimeMillis(),
    val waterAmountLiters: Int = 0,
    val measuredTemp: Double? = null,
    val measuredPh: Double? = null,
    val measuredGh: Int? = null,
    val measuredNo3: Int? = null,
    val title: String,
    val notes: String = ""
)

data class FishRecognitionResult(
    val hungarianName: String,
    val scientificName: String,
    val family: String = "",
    val confidencePercent: Int,
    val uncertaintyWarning: String = "",
    val alternativeSpecies: List<String> = emptyList(),
    val size: String = "",
    val habitat: String = "",
    val temperature: String = "",
    val ph: String = "",
    val hardness: String = "",
    val diet: String = "",
    val compatibility: String = "",
    val breeding: String = "",
    val description: String = "",
    val careTips: String = ""
)
