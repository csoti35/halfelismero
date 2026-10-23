package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.local.FishDao
import com.example.data.local.InitialFishData
import com.example.data.model.Aquarium
import com.example.data.model.AquariumFish
import com.example.data.model.CareLogEntry
import com.example.data.model.FishRecognitionResult
import com.example.data.model.FishSpecies
import com.example.data.remote.GeminiFishIdentifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AquariumRepository(
    private val fishDao: FishDao,
    private val geminiFishIdentifier: GeminiFishIdentifier = GeminiFishIdentifier()
) {
    val allSpecies: Flow<List<FishSpecies>> = fishDao.getAllSpecies()
    val allAquariums: Flow<List<Aquarium>> = fishDao.getAllAquariums()
    val allCareLogs: Flow<List<CareLogEntry>> = fishDao.getAllCareLogs()

    fun searchSpecies(query: String): Flow<List<FishSpecies>> {
        return if (query.isBlank()) {
            fishDao.getAllSpecies()
        } else {
            fishDao.searchSpecies(query)
        }
    }

    fun getFishForAquarium(aquariumId: Long): Flow<List<AquariumFish>> =
        fishDao.getFishForAquarium(aquariumId)

    fun getCareLogsForAquarium(aquariumId: Long): Flow<List<CareLogEntry>> =
        fishDao.getCareLogsForAquarium(aquariumId)

    suspend fun insertSpecies(species: FishSpecies): Long = fishDao.insertSpecies(species)

    suspend fun insertAquarium(aquarium: Aquarium): Long = fishDao.insertAquarium(aquarium)

    suspend fun updateAquarium(aquarium: Aquarium) = fishDao.updateAquarium(aquarium)

    suspend fun deleteAquarium(id: Long) = fishDao.deleteAquariumById(id)

    suspend fun addFishToAquarium(fish: AquariumFish): Long = fishDao.insertAquariumFish(fish)

    suspend fun removeFishFromAquarium(id: Long) = fishDao.deleteAquariumFishById(id)

    suspend fun addCareLog(log: CareLogEntry): Long = fishDao.insertCareLog(log)

    suspend fun deleteCareLog(log: CareLogEntry) = fishDao.deleteCareLog(log)

    suspend fun identifyFish(bitmap: Bitmap): Result<FishRecognitionResult> {
        return geminiFishIdentifier.identifyFish(bitmap)
    }

    /**
     * Fallback or sample demo recognition from offline catalog
     */
    suspend fun sampleDemoIdentify(): FishRecognitionResult {
        val speciesList = InitialFishData.defaultSpecies
        val sample = speciesList.random()
        val altList = speciesList.filter { it.hungarianName != sample.hungarianName }.take(2).map { it.hungarianName }
        return FishRecognitionResult(
            hungarianName = sample.hungarianName,
            scientificName = sample.scientificName,
            family = sample.family,
            confidencePercent = 94,
            uncertaintyWarning = "",
            alternativeSpecies = altList,
            size = sample.sizeCm,
            habitat = sample.originHabitat,
            temperature = "${sample.minTempC} – ${sample.maxTempC} °C",
            ph = "${sample.minPh} – ${sample.maxPh}",
            hardness = "${sample.minGh} – ${sample.maxGh} dGH",
            diet = sample.diet,
            compatibility = sample.temperament,
            breeding = sample.breedingInfo,
            description = sample.description,
            careTips = sample.careTips
        )
    }

    suspend fun saveIdentifiedSpeciesToDatabase(result: FishRecognitionResult): Long {
        val species = FishSpecies(
            hungarianName = result.hungarianName,
            scientificName = result.scientificName,
            family = result.family.ifBlank { "Egyéb család" },
            sizeCm = result.size.ifBlank { "Nincs adat" },
            originHabitat = result.habitat.ifBlank { "Dél-Amerika / Ázsia" },
            minTempC = 22,
            maxTempC = 28,
            minPh = 6.5,
            maxPh = 7.5,
            minGh = 4,
            maxGh = 15,
            diet = result.diet.ifBlank { "Mindenevő" },
            temperament = result.compatibility.ifBlank { "Békés" },
            breedingInfo = result.breeding.ifBlank { "Adatbázisban részletezve" },
            description = result.description,
            careTips = result.careTips,
            isPreloaded = false
        )
        return fishDao.insertSpecies(species)
    }
}
