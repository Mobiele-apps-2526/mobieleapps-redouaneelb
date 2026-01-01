package com.example.osso.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.osso.models.House
import com.example.osso.repositories.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HouseViewModel(private val repository: HouseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseUiState())
    val uiState: StateFlow<HouseUiState> = _uiState.asStateFlow()

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Home)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private var lastSwipedHouse: House? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // Listener for the main house stack
        viewModelScope.launch {
            repository.getHouses().collect { houses ->
                _uiState.update { it.copy(
                    houses = houses.filterNot { house -> house.isSwiped },
                    isLoading = false
                ) }
            }
        }

        // The single, persistent listener for liked houses. This is the ONLY source of truth.
        viewModelScope.launch {
            repository.getLikedHouses(getCurrentUserId()).collect { likedHouses ->
                _uiState.update { it.copy(likedHouses = likedHouses) }
            }
        }
    }

    fun likeHouse(house: House) {
        viewModelScope.launch {
            lastSwipedHouse = house
            // FIX: Immediately update the swiping stack locally for a snappy UI.
            _uiState.update { currentState ->
                currentState.copy(houses = currentState.houses.filterNot { it.id == house.id })
            }
            // The persistent listener in init{} will handle updating the likedHouses list from the server.
            repository.likeHouse(house.id, getCurrentUserId())
        }
    }

    fun dislikeHouse(house: House) {
        viewModelScope.launch {
            lastSwipedHouse = house
            // FIX: Immediately update the swiping stack locally.
            _uiState.update { currentState ->
                currentState.copy(houses = currentState.houses.filterNot { it.id == house.id })
            }
            repository.dislikeHouse(house.id, getCurrentUserId())
        }
    }
    
    fun removeFromFavorites(house: House) {
        viewModelScope.launch {
            // Only write to the repository. The persistent listener will update the UI.
            repository.dislikeHouse(house.id, getCurrentUserId())
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun undoLastSwipe() {
        lastSwipedHouse?.let {
            _uiState.update { currentState ->
                currentState.copy(houses = currentState.houses + it)
            }
            lastSwipedHouse = null
        }
    }

    fun navigateToLikedHouses() {
        _navigationState.value = NavigationState.LikedHouses
    }

    fun navigateToHome() {
        _navigationState.value = NavigationState.Home
    }

    fun navigateToMap() {
        _navigationState.value = NavigationState.Map
    }

    fun navigateToChat() {
        _navigationState.value = NavigationState.Chat
    }

    private fun getCurrentUserId(): String {
        return "user_123"
    }
}

data class HouseUiState(
    val houses: List<House> = emptyList(),
    val likedHouses: List<House> = emptyList(),
    val isLoading: Boolean = true,
    val selectedFilter: String = "Alles",
    val searchQuery: String = ""
) {
    val filteredLikedHouses: List<House>
        get() = if (selectedFilter.equals("Alles", ignoreCase = true)) {
            likedHouses
        } else {
            likedHouses.filter { it.propertyType.equals(selectedFilter, ignoreCase = true) }
        }

    val searchedAndFilteredLikedHouses: List<House>
        get() {
            val categoryFiltered = filteredLikedHouses
            return if (searchQuery.isBlank()) {
                categoryFiltered
            } else {
                categoryFiltered.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.address.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.propertyType.contains(searchQuery, ignoreCase = true)
                }
            }
        }
}

sealed class NavigationState {
    object Home : NavigationState()
    object LikedHouses : NavigationState()
    object Map : NavigationState()
    object Chat : NavigationState()
}
