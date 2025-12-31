package com.example.osso.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.osso.ViewModel.HouseViewModel
import com.example.osso.models.House
import com.example.osso.view.theme.HeartRed
import com.example.osso.view.theme.OssoBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: HouseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoriteHouses = uiState.filteredLikedHouses // Use the filtered list
    val filters = listOf("Alles", "Huis", "Appartement", "Studio", "Kot")

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mijn Favorieten",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF212121)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min) // Ensure all chips have the same height
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        modifier = Modifier.fillMaxHeight(),
                        selected = uiState.selectedFilter.equals(filter, ignoreCase = true),
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter) }
                    )
                }
            }

            if (favoriteHouses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.selectedFilter != "Alles") "Geen resultaten" else "Geen favorieten",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.selectedFilter != "Alles") "Probeer een ander filter" else "Swipe rechts om huizen op te slaan",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favoriteHouses, key = { it.id }) { house ->
                        FavoriteHouseCard(
                            house = house,
                            onRemoveFavorite = { viewModel.removeFromFavorites(house) },
                            onClick = { onNavigateToDetails(house.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteHouseCard(
    house: House,
    onRemoveFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row {
            // Image
            AsyncImage(
                model = house.imageUrls.firstOrNull() ?: "",
                contentDescription = house.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )

            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = house.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    color = Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "€ ${String.format("%,d", house.price)}",
                    fontSize = 18.sp,
                    color = OssoBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PropertyInfo(icon = Icons.Default.Bed, value = "${house.bedrooms}")
                    PropertyInfo(icon = Icons.Default.Bathtub, value = "${house.bathrooms}")
                    PropertyInfo(icon = Icons.Default.SquareFoot, value = "${house.squareFootage}m²")
                }
            }

            // Favorite button
            IconButton(
                onClick = onRemoveFavorite,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = HeartRed
                )
            }
        }
    }
}

@Composable
fun PropertyInfo(
    icon: ImageVector,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
