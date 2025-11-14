
package com.example.osso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
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
        bottomBar = { OssoBottomBar(navState, viewModel) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (navState) {
                is NavigationState.Home -> HomeScreen(uiState, viewModel)
                is NavigationState.LikedHouses -> LikedScreen(uiState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OssoTopAppBar() {
    TopAppBar(
        title = {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.height(32.dp)
            )
        },
        actions = {
            IconButton(onClick = { /* TODO: Implement filter */ }) {
                Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter")
            }
        }
    )
}

@Composable
fun OssoBottomBar(navState: NavigationState, viewModel: HouseViewModel) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = navState is NavigationState.Home,
            onClick = { viewModel.navigateToHome() }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Liked") },
            label = { Text("Liked") },
            selected = navState is NavigationState.LikedHouses,
            onClick = { viewModel.navigateToLikedHouses() }
        )
    }
}

@Composable
fun HomeScreen(uiState: HouseUiState, viewModel: HouseViewModel) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val houses = uiState.houses
    if (houses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No more houses! Check back later.", fontSize = 18.sp)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        houses.reversed().forEach { house ->
            CustomSwipeableCard(house = house, onSwipe = {
                if (it > 0) viewModel.likeHouse(house) else viewModel.dislikeHouse(house)
            })
        }
    }
}

@Composable
fun CustomSwipeableCard(house: House, onSwipe: (direction: Int) -> Unit) {
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
        HouseCard(house = house)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseCard(house: House) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            val pagerState = rememberPagerState { house.imageUrls.size }
            HorizontalPager(state = pagerState) {
                page ->
                AsyncImage(
                    model = house.imageUrls[page],
                    contentDescription = "House Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 600f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "€ ${house.price}",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = house.address,
                    color = Color.White,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.White else Color.Gray
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
