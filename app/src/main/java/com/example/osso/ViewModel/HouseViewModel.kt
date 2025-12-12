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
        loadHouses()
    }

    private fun loadHouses() {
        viewModelScope.launch {
            repository.getHouses().collect { houses ->
                _uiState.update { it.copy(
                    houses = houses.filterNot { house -> house.isSwiped },
                    isLoading = false
                ) }
            }
        }
    }

    fun likeHouse(house: House) {
        viewModelScope.launch {
            lastSwipedHouse = house
            repository.likeHouse(house.id, getCurrentUserId())
            _uiState.update { currentState ->
                currentState.copy(
                    houses = currentState.houses.filterNot { it.id == house.id },
                    likedHouses = currentState.likedHouses + house
                )
            }
        }
    }

    fun dislikeHouse(house: House) {
        viewModelScope.launch {
            lastSwipedHouse = house
            repository.dislikeHouse(house.id, getCurrentUserId())
            _uiState.update { currentState ->
                currentState.copy(
                    houses = currentState.houses.filterNot { it.id == house.id }
                )
            }
        }
    }
    
    fun removeFromFavorites(house: House) {
        viewModelScope.launch {
            repository.dislikeHouse(house.id, getCurrentUserId())
            _uiState.update { currentState ->
                currentState.copy(
                    likedHouses = currentState.likedHouses.filterNot { it.id == house.id }
                )
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun undoLastSwipe() {
        lastSwipedHouse?.let {
            _uiState.update { currentState ->
                currentState.copy(houses = currentState.houses + it)
            }
            lastSwipedHouse = null // Prevent multiple undos
        }
    }

    fun navigateToLikedHouses() {
        viewModelScope.launch {
            repository.getLikedHouses(getCurrentUserId()).collect { likedHouses ->
                _uiState.update { it.copy(likedHouses = likedHouses) }
            }
        }
        _navigationState.value = NavigationState.LikedHouses
    }

    fun navigateToHome() {
        _navigationState.value = NavigationState.Home
    }

    private fun getCurrentUserId(): String {
        // In a real app, this would come from authentication
        return "user_123"
    }
}

data class HouseUiState(
    val houses: List<House> = emptyList(),
    val likedHouses: List<House> = emptyList(),
    val isLoading: Boolean = true,
    val selectedFilter: String = "Alles"
) {
    val filteredLikedHouses: List<House>
        get() = if (selectedFilter == "Alles") {
            likedHouses
        } else {
            likedHouses.filter { it.propertyType.equals(selectedFilter, ignoreCase = true) }
        }
}

sealed class NavigationState {
    object Home : NavigationState()
    object LikedHouses : NavigationState()
}