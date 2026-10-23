package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Aquarium
import com.example.data.model.AquariumFish
import com.example.data.model.CareLogEntry
import com.example.data.model.FishSpecies
import kotlinx.coroutines.flow.Flow

@Dao
interface FishDao {

    // Fish Species
    @Query("SELECT * FROM fish_species ORDER BY hungarianName ASC")
    fun getAllSpecies(): Flow<List<FishSpecies>>

    @Query("""
        SELECT * FROM fish_species 
        WHERE hungarianName LIKE '%' || :query || '%' 
           OR scientificName LIKE '%' || :query || '%'
           OR family LIKE '%' || :query || '%'
        ORDER BY hungarianName ASC
    """)
    fun searchSpecies(query: String): Flow<List<FishSpecies>>

    @Query("SELECT * FROM fish_species WHERE id = :id LIMIT 1")
    suspend fun getSpeciesById(id: Long): FishSpecies?

    @Query("SELECT COUNT(*) FROM fish_species")
    suspend fun getSpeciesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecies(species: FishSpecies): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSpecies(speciesList: List<FishSpecies>)

    // Aquariums
    @Query("SELECT * FROM aquariums ORDER BY createdAt DESC")
    fun getAllAquariums(): Flow<List<Aquarium>>

    @Query("SELECT * FROM aquariums WHERE id = :id LIMIT 1")
    suspend fun getAquariumById(id: Long): Aquarium?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAquarium(aquarium: Aquarium): Long

    @Update
    suspend fun updateAquarium(aquarium: Aquarium)

    @Delete
    suspend fun deleteAquarium(aquarium: Aquarium)

    @Query("DELETE FROM aquariums WHERE id = :id")
    suspend fun deleteAquariumById(id: Long)

    // Aquarium Fish Stock
    @Query("SELECT * FROM aquarium_fish WHERE aquariumId = :aquariumId ORDER BY speciesName ASC")
    fun getFishForAquarium(aquariumId: Long): Flow<List<AquariumFish>>

    @Query("SELECT * FROM aquarium_fish ORDER BY speciesName ASC")
    fun getAllStockedFish(): Flow<List<AquariumFish>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAquariumFish(fish: AquariumFish): Long

    @Delete
    suspend fun deleteAquariumFish(fish: AquariumFish)

    @Query("DELETE FROM aquarium_fish WHERE id = :id")
    suspend fun deleteAquariumFishById(id: Long)

    // Care Logs
    @Query("SELECT * FROM care_logs ORDER BY timestamp DESC")
    fun getAllCareLogs(): Flow<List<CareLogEntry>>

    @Query("SELECT * FROM care_logs WHERE aquariumId = :aquariumId ORDER BY timestamp DESC")
    fun getCareLogsForAquarium(aquariumId: Long): Flow<List<CareLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCareLog(log: CareLogEntry): Long

    @Delete
    suspend fun deleteCareLog(log: CareLogEntry)
}
