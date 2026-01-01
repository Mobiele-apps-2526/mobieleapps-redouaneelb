package com.example.osso.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.osso.ViewModel.HouseUiState
import com.example.osso.ViewModel.HouseViewModel
import com.example.osso.models.House

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(uiState: HouseUiState, viewModel: HouseViewModel) {
    val filters = listOf("Alles", "Huis", "Appartement", "Studio")
    // Use the new computed property for searching
    val housesToShow = uiState.searchedAndFilteredLikedHouses

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(80.dp))

        // Search Bar - now connected to the ViewModel
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Zoek prijs, locatie, titel,...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape
        )

        Spacer(Modifier.height(16.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = uiState.selectedFilter.equals(filter, ignoreCase = true),
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(text = filter, maxLines = 1) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Alles wat jij leuk vindt", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))

        // Liked Houses Grid - uses the new list
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(housesToShow, key = { it.id }) {
                house ->
                FavoriteGridItem(house = house, onDislike = { viewModel.removeFromFavorites(house) })
            }
        }
    }
}

@Composable
fun FavoriteGridItem(house: House, onDislike: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.height(220.dp)) {
            AsyncImage(
                model = house.imageUrls.firstOrNull(),
                contentDescription = house.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 300f
                        )
                    )
            )

            // Favorite Button
            IconButton(
                onClick = onDislike,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = "Dislike", tint = Color(0xFF0091EA))
            }
            
            // House Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(house.propertyType, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${house.bedrooms} slp ‖ ${house.squareFootage} m²", color = Color.White, fontSize = 14.sp)
                Text(house.address, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
