
package com.example.osso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.example.osso.ViewModel.HouseViewModel
import com.example.osso.ViewModel.HouseUiState
import com.example.osso.ViewModel.NavigationState
import com.example.osso.models.House
import com.example.osso.repositories.HouseRepository
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val viewModel: HouseViewModel by viewModels { HouseViewModelFactory(HouseRepository()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OssoApp(viewModel)
        }
    }
}

class HouseViewModelFactory(private val repository: HouseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HouseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HouseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun OssoApp(viewModel: HouseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navState by viewModel.navigationState.collectAsState()

    Scaffold(
        topBar = { OssoTopAppBar() },
        bottomBar = { OssoBottomBar(navState, viewModel) },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (navState) {
                    is NavigationState.Home -> HomeScreen(uiState, viewModel)
                    is NavigationState.LikedHouses -> LikedScreen(uiState)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OssoTopAppBar() {
    TopAppBar(
        title = { OssoLogo() },
        actions = {
            IconButton(onClick = { /* TODO: Implement filter */ }) {
                Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
fun OssoLogo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Placeholder
            contentDescription = "Osso Logo",
            tint = Color(0xFF0091EA),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "OSSO",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0091EA)
        )
    }
}

@Composable
fun OssoBottomBar(navState: NavigationState, viewModel: HouseViewModel) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
        )
        NavigationBarItem(
            selected = navState is NavigationState.LikedHouses,
            onClick = { viewModel.navigateToLikedHouses() },
            icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF0091EA), unselectedIconColor = Color.Gray)
        )
        NavigationBarItem(
            selected = navState is NavigationState.Home,
            onClick = { viewModel.navigateToHome() },
            icon = { Icon(Icons.Default.Layers, contentDescription = "Home") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF0091EA), unselectedIconColor = Color.Gray)
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Messages") },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO */ },
            icon = { Icon(Icons.Default.PersonOutline, contentDescription = "Profile") },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
        )
    }
}

@Composable
fun HomeScreen(uiState: HouseUiState, viewModel: HouseViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        val houses = uiState.houses
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
             if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (houses.isEmpty()) {
                Text("No more houses! Check back later.", fontSize = 18.sp)
            } else {
                 val currentHouse = houses.last()
                CustomSwipeableCard(house = currentHouse, onSwipe = {
                    if (it > 0) viewModel.likeHouse(currentHouse) else viewModel.dislikeHouse(currentHouse)
                })
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (houses.isNotEmpty()) {
                ActionButton(icon = Icons.Default.Close, color = Color.Red, onClick = { viewModel.dislikeHouse(houses.last()) })
                ActionButton(icon = Icons.Default.Refresh, color = Color.Gray, onClick = { viewModel.undoLastSwipe() })
                ActionButton(icon = Icons.Default.Favorite, color = Color.Green, onClick = { viewModel.likeHouse(houses.last()) })
            }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color.White,
        contentColor = color,
        shape = CircleShape,
        modifier = Modifier.size(56.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun CustomSwipeableCard(
    house: House,
    onSwipe: (direction: Int) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .offset(x = (offsetX).dp)
            .graphicsLayer(
                rotationZ = (offsetX / 50f).coerceIn(-15f, 15f),
                alpha = (1 - abs(offsetX) / 400f).coerceIn(0f, 1f)
            )
            .pointerInput(house.id) { // Use house.id as the key
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            when {
                                offsetX > 300 -> onSwipe(1) // Swiped Right
                                offsetX < -300 -> onSwipe(-1) // Swiped Left
                            }
                            offsetX = 0f
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                }
            }
    ) {
        HouseCard(house = house, offsetX = offsetX)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseCard(house: House, offsetX: Float) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            val pagerState = rememberPagerState { house.imageUrls.size }
            val coroutineScope = rememberCoroutineScope()

            HorizontalPager(state = pagerState) {
                page ->
                AsyncImage(
                    model = house.imageUrls[page],
                    contentDescription = "House Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 800f
                        )
                    )
            )

            // Clickable areas for image navigation
            Row(Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { 
                    coroutineScope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                })
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { 
                    coroutineScope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(house.imageUrls.size - 1))
                    }
                })
            }

            // Swipe indicators (Heart and X)
            val likeAlpha by animateFloatAsState(targetValue = if (offsetX > 0) (offsetX / 300f).coerceIn(0f, 1f) else 0f)
            val dislikeAlpha by animateFloatAsState(targetValue = if (offsetX < 0) (abs(offsetX) / 300f).coerceIn(0f, 1f) else 0f)

            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Like",
                tint = Color.Green.copy(alpha = likeAlpha),
                modifier = Modifier.align(Alignment.Center).size(100.dp)
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dislike",
                tint = Color.Red.copy(alpha = dislikeAlpha),
                modifier = Modifier.align(Alignment.Center).size(100.dp)
            )

            // Price Chip
            Card(
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0091EA))
            ) {
                Text(
                    text = "€ ${String.format("%,d", house.price)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Content at the bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = house.propertyType, // "Huis"
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = house.address,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Info Tags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoTag("${house.bedrooms} Slaapkamers")
                    InfoTag("${house.bathrooms} Badkamer")
                    InfoTag("${house.squareFootage}m² Woonbaar")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    InfoTag("${house.gardenSize}m² Tuin")
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Pager indicator
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.White else Color.Gray.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoTag(text: String) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LikedScreen(uiState: HouseUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(uiState.likedHouses) { house ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = house.imageUrls.firstOrNull(),
                        contentDescription = "House Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(100.dp)
                    )
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = house.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "€ ${house.price}", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
