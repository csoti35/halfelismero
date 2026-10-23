package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Aquarium
import com.example.data.model.AquariumFish
import com.example.data.model.CareLogEntry
import com.example.data.model.FishSpecies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FishSpecies::class,
        Aquarium::class,
        AquariumFish::class,
        CareLogEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fishDao(): FishDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aquarium_master_db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialDatabase(database.fishDao())
                    }
                }
            }

            suspend fun populateInitialDatabase(dao: FishDao) {
                // Populate default species encyclopedia
                dao.insertAllSpecies(InitialFishData.defaultSpecies)

                // Populate starter aquarium
                val starterTankId = dao.insertAquarium(
                    Aquarium(
                        name = "Fő társas akvárium",
                        volumeLiters = 120,
                        currentTempC = 25.5,
                        currentPh = 6.8,
                        currentGh = 7,
                        currentNo3 = 10,
                        substrate = "ADA Aqua Soil + finom kvarchomok",
                        filterType = "Eheim eXperience 250 külső szűrő",
                        notes = "Növényes dél-amerikai biotóp jellegű társas akvárium."
                    )
                )

                // Add starter fish stock
                dao.insertAquariumFish(
                    AquariumFish(
                        aquariumId = starterTankId,
                        speciesName = "Neonhal",
                        scientificName = "Paracheirodon innesi",
                        count = 15,
                        notes = "Csapatban úsznak a középső vízrétegben."
                    )
                )
                dao.insertAquariumFish(
                    AquariumFish(
                        aquariumId = starterTankId,
                        speciesName = "Bronzpáncélos harcsa",
                        scientificName = "Corydoras aeneus",
                        count = 6,
                        notes = "Fenéktakarítók, aktívak etetéskor."
                    )
                )
                dao.insertAquariumFish(
                    AquariumFish(
                        aquariumId = starterTankId,
                        speciesName = "Algaevő harcsa (Ancistrus)",
                        scientificName = "Ancistrus cirrhosus",
                        count = 2,
                        notes = "Fadarabokon pihennek, algát tisztítanak."
                    )
                )

                // Add initial care logs
                val now = System.currentTimeMillis()
                dao.insertCareLog(
                    CareLogEntry(
                        aquariumId = starterTankId,
                        aquariumName = "Fő társas akvárium",
                        type = "WATER_CHANGE",
                        timestamp = now - (3 * 24 * 3600 * 1000L),
                        waterAmountLiters = 35,
                        measuredTemp = 25.5,
                        measuredPh = 6.8,
                        title = "Heti részleges vízcsere",
                        notes = "30% pihentetett víz csere, aljzatporszívózás és szűrő előszivacs öblítés."
                    )
                )
                dao.insertCareLog(
                    CareLogEntry(
                        aquariumId = starterTankId,
                        aquariumName = "Fő társas akvárium",
                        type = "WATER_TEST",
                        timestamp = now - (1 * 24 * 3600 * 1000L),
                        measuredTemp = 25.4,
                        measuredPh = 6.9,
                        measuredGh = 8,
                        measuredNo3 = 12,
                        title = "Vízteszt ellenőrzés",
                        notes = "A paraméterek ideálisak a halak és a növények számára."
                    )
                )
            }
        }
    }
}
