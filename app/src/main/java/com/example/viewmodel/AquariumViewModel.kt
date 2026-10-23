package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.model.Aquarium
import com.example.data.model.AquariumFish
import com.example.data.model.CareLogEntry
import com.example.data.model.FishRecognitionResult
import com.example.data.model.FishSpecies
import com.example.data.repository.AquariumRepository
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    HOME("Főoldal"),
    IDENTIFY("Halfelismerés"),
    DATABASE("Halfaj-adatbázis"),
    MY_AQUARIUMS("Akváriumaim"),
    CARE_LOG("Gondozási napló"),
    SETTINGS("Beállítások")
}

data class RecognitionUiState(
    val imageBitmap: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val result: FishRecognitionResult? = null,
    val errorMessage: String? = null,
    val isApiKeyMissing: Boolean = false,
    val hasSaved: Boolean = false,
    val isSimulatedDemo: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AquariumViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = AquariumRepository(database.fishDao())

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Recognition State
    private val _recognitionState = MutableStateFlow(RecognitionUiState())
    val recognitionState: StateFlow<RecognitionUiState> = _recognitionState.asStateFlow()

    // Species Encyclopedia & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFamilyFilter = MutableStateFlow<String?>(null)
    val selectedFamilyFilter: StateFlow<String?> = _selectedFamilyFilter.asStateFlow()

    val filteredSpecies: StateFlow<List<FishSpecies>> = combine(
        _searchQuery.flatMapLatest { query -> repository.searchSpecies(query) },
        _selectedFamilyFilter
    ) { speciesList, familyFilter ->
        if (familyFilter == null || familyFilter == "Összes") {
            speciesList
        } else {
            speciesList.filter { it.family.contains(familyFilter, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedSpeciesForDetail = MutableStateFlow<FishSpecies?>(null)
    val selectedSpeciesForDetail: StateFlow<FishSpecies?> = _selectedSpeciesForDetail.asStateFlow()

    fun selectSpeciesForDetail(species: FishSpecies?) {
        _selectedSpeciesForDetail.value = species
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFamilyFilter(family: String?) {
        _selectedFamilyFilter.value = family
    }

    // Aquariums
    val aquariums: StateFlow<List<Aquarium>> = repository.allAquariums.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedAquarium = MutableStateFlow<Aquarium?>(null)
    val selectedAquarium: StateFlow<Aquarium?> = _selectedAquarium.asStateFlow()

    val currentTankFish: StateFlow<List<AquariumFish>> = _selectedAquarium.flatMapLatest { tank ->
        if (tank != null) {
            repository.getFishForAquarium(tank.id)
        } else {
            MutableStateFlow(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectAquarium(aquarium: Aquarium?) {
        _selectedAquarium.value = aquarium
    }

    fun addAquarium(name: String, liters: Int, temp: Double, ph: Double, gh: Int, no3: Int, notes: String) {
        viewModelScope.launch {
            val id = repository.insertAquarium(
                Aquarium(
                    name = name,
                    volumeLiters = liters,
                    currentTempC = temp,
                    currentPh = ph,
                    currentGh = gh,
                    currentNo3 = no3,
                    notes = notes
                )
            )
            // Select newly added aquarium
            _selectedAquarium.value = Aquarium(
                id = id,
                name = name,
                volumeLiters = liters,
                currentTempC = temp,
                currentPh = ph,
                currentGh = gh,
                currentNo3 = no3,
                notes = notes
            )
        }
    }

    fun deleteAquarium(aquarium: Aquarium) {
        viewModelScope.launch {
            repository.deleteAquarium(aquarium.id)
            if (_selectedAquarium.value?.id == aquarium.id) {
                _selectedAquarium.value = null
            }
        }
    }

    fun addFishToTank(aquariumId: Long, name: String, scientificName: String, count: Int, notes: String) {
        viewModelScope.launch {
            repository.addFishToAquarium(
                AquariumFish(
                    aquariumId = aquariumId,
                    speciesName = name,
                    scientificName = scientificName,
                    count = count,
                    notes = notes
                )
            )
        }
    }

    fun removeFishFromTank(fishId: Long) {
        viewModelScope.launch {
            repository.removeFishFromAquarium(fishId)
        }
    }

    // Care Logs
    val careLogs: StateFlow<List<CareLogEntry>> = repository.allCareLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _logFilter = MutableStateFlow("ALL")
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun addCareLog(
        aquariumId: Long,
        aquariumName: String,
        type: String,
        title: String,
        waterAmount: Int = 0,
        temp: Double? = null,
        ph: Double? = null,
        gh: Int? = null,
        no3: Int? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.addCareLog(
                CareLogEntry(
                    aquariumId = aquariumId,
                    aquariumName = aquariumName,
                    type = type,
                    waterAmountLiters = waterAmount,
                    measuredTemp = temp,
                    measuredPh = ph,
                    measuredGh = gh,
                    measuredNo3 = no3,
                    title = title,
                    notes = notes
                )
            )
        }
    }

    fun deleteCareLog(log: CareLogEntry) {
        viewModelScope.launch {
            repository.deleteCareLog(log)
        }
    }

    // AI Recognition Actions
    fun setImageFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()

                // Check EXIF orientation
                var rotationDegrees = 0
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        rotationDegrees = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                    }
                } catch (e: Throwable) {
                    // Ignore exif parsing failure
                }

                // Decode bounds first to prevent OutOfMemory
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }

                val maxDimension = 1600
                var inSampleSize = 1
                while (boundsOptions.outWidth / inSampleSize > maxDimension || boundsOptions.outHeight / inSampleSize > maxDimension) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = inSampleSize }
                val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                if (decodedBitmap != null) {
                    val finalBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
                    } else {
                        decodedBitmap
                    }

                    _recognitionState.value = RecognitionUiState(
                        imageBitmap = finalBitmap,
                        isAnalyzing = false,
                        result = null,
                        errorMessage = null
                    )
                } else {
                    _recognitionState.value = _recognitionState.value.copy(
                        errorMessage = "A kiválasztott fájl nem érvényes kép."
                    )
                }
            } catch (e: Exception) {
                _recognitionState.value = _recognitionState.value.copy(
                    errorMessage = "Nem sikerült betölteni a képet: ${e.localizedMessage}"
                )
            }
        }
    }

    fun setImageBitmap(bitmap: Bitmap) {
        _recognitionState.value = RecognitionUiState(
            imageBitmap = bitmap,
            isAnalyzing = false,
            result = null,
            errorMessage = null
        )
    }

    fun startIdentification() {
        val bitmap = _recognitionState.value.imageBitmap
        if (bitmap == null) {
            _recognitionState.value = _recognitionState.value.copy(
                errorMessage = "Kérjük előbb készíts vagy válassz ki egy fotót a halról!"
            )
            return
        }

        _recognitionState.value = _recognitionState.value.copy(
            isAnalyzing = true,
            errorMessage = null,
            result = null,
            hasSaved = false
        )

        viewModelScope.launch {
            val result = repository.identifyFish(bitmap)
            result.onSuccess { fishResult ->
                _recognitionState.value = _recognitionState.value.copy(
                    isAnalyzing = false,
                    result = fishResult,
                    errorMessage = null,
                    isApiKeyMissing = false,
                    isSimulatedDemo = false
                )
            }.onFailure { error ->
                val isMissingKey = error is com.example.data.remote.MissingApiKeyException ||
                        com.example.data.local.ApiKeyStore.effectiveKey().isBlank()

                _recognitionState.value = _recognitionState.value.copy(
                    isAnalyzing = false,
                    errorMessage = error.localizedMessage ?: "Ismeretlen hiba történt a felismerés során.",
                    isApiKeyMissing = isMissingKey
                )
            }
        }
    }

    fun runSampleDemoIdentification() {
        _recognitionState.value = _recognitionState.value.copy(
            isAnalyzing = true,
            errorMessage = null,
            result = null,
            hasSaved = false
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(800) // Brief realistic animation
            val sampleResult = repository.sampleDemoIdentify()
            _recognitionState.value = _recognitionState.value.copy(
                isAnalyzing = false,
                result = sampleResult,
                errorMessage = null,
                isApiKeyMissing = false,
                isSimulatedDemo = true
            )
        }
    }

    fun saveIdentifiedFishToDatabase() {
        val res = _recognitionState.value.result ?: return
        viewModelScope.launch {
            repository.saveIdentifiedSpeciesToDatabase(res)
            _recognitionState.value = _recognitionState.value.copy(hasSaved = true)
        }
    }

    fun addIdentifiedFishToAquarium(aquariumId: Long, count: Int = 1) {
        val res = _recognitionState.value.result ?: return
        viewModelScope.launch {
            repository.addFishToAquarium(
                AquariumFish(
                    aquariumId = aquariumId,
                    speciesName = res.hungarianName,
                    scientificName = res.scientificName,
                    count = count,
                    notes = "Hozzáadva a halfelismerőből."
                )
            )
        }
    }

    fun resetRecognition() {
        _recognitionState.value = RecognitionUiState()
    }
}
