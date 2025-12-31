package com.example.osso.view.screens

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.osso.ViewModel.HouseUiState
import com.example.osso.ViewModel.HouseViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

private const val darkMapStyleJson = """[{"elementType":"geometry","stylers":[{"color":"#212121"}]},{"elementType":"labels.icon","stylers":[{"visibility":"off"}]},{"elementType":"labels.text.fill","stylers":[{"color":"#757575"}]},{"elementType":"labels.text.stroke","stylers":[{"color":"#212121"}]},{"featureType":"administrative","elementType":"geometry","stylers":[{"color":"#757575"}]},{"featureType":"administrative.country","elementType":"labels.text.fill","stylers":[{"color":"#9e9e9e"}]},{"featureType":"administrative.land_parcel","stylers":[{"visibility":"off"}]},{"featureType":"administrative.locality","elementType":"labels.text.fill","stylers":[{"color":"#bdbdbd"}]},{"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#757575"}]},{"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#181818"}]},{"featureType":"poi.park","elementType":"labels.text.fill","stylers":[{"color":"#616161"}]},{"featureType":"poi.park","elementType":"labels.text.stroke","stylers":[{"color":"#1b1b1b"}]},{"featureType":"road","elementType":"geometry.fill","stylers":[{"color":"#2c2c2c"}]},{"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#8a8a8a"}]},{"featureType":"road.arterial","elementType":"geometry","stylers":[{"color":"#373737"}]},{"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#3c3c3c"}]},{"featureType":"road.highway.controlled_access","elementType":"geometry","stylers":[{"color":"#4e4e4e"}]},{"featureType":"road.local","elementType":"labels.text.fill","stylers":[{"color":"#616161"}]},{"featureType":"transit","elementType":"labels.text.fill","stylers":[{"color":"#757575"}]},{"featureType":"water","elementType":"geometry","stylers":[{"color":"#000000"}]},{"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#3d3d3d"}]}]"""

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission") 
@Composable
fun MapScreen(uiState: HouseUiState, viewModel: HouseViewModel) {
    val housesToDisplay = uiState.filteredLikedHouses // Use the filtered list
    val coroutineScope = rememberCoroutineScope()

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(50.8466, 4.3525), 10f) // Default to Brussels
    }

    LaunchedEffect(housesToDisplay, locationPermissions.allPermissionsGranted) {
        if (housesToDisplay.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            housesToDisplay.forEach { house ->
                boundsBuilder.include(LatLng(house.latitude, house.longitude))
            }
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100),
                durationMs = 1500
            )
        }
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    val mapStyle = MapStyleOptions(darkMapStyleJson)
    val mapProperties by remember(locationPermissions.allPermissionsGranted) { 
        mutableStateOf(MapProperties(isMyLocationEnabled = locationPermissions.allPermissionsGranted, mapStyleOptions = mapStyle)) 
    }
    val uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true, compassEnabled = true, myLocationButtonEnabled = false)) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            housesToDisplay.forEach { house ->
                if (house.latitude != 0.0 && house.longitude != 0.0) {
                    Marker(
                        state = MarkerState(position = LatLng(house.latitude, house.longitude)),
                        title = house.title,
                        snippet = "€ ${house.price}"
                    )
                } else {
                    Log.w("MapScreen", "House '${house.title}' has invalid coordinates.")
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    if (housesToDisplay.isNotEmpty()) {
                        coroutineScope.launch {
                            val boundsBuilder = LatLngBounds.builder()
                            housesToDisplay.forEach { house ->
                                boundsBuilder.include(LatLng(house.latitude, house.longitude))
                            }
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150),
                                durationMs = 1000
                            )
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Center on Favorites")
            }

            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        if (locationPermissions.allPermissionsGranted) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    val latLng = LatLng(it.latitude, it.longitude)
                                    coroutineScope.launch { 
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                    }
                                }
                            }
                        } else {
                            locationPermissions.launchMultiplePermissionRequest()
                        }
                    }
                }
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on my location")
            }
        }
    }
}