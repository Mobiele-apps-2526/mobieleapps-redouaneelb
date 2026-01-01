package com.example.osso.models
data class House(
    val id: String = "",
    val title: String = "",
    val price: Int = 0,
    val address: String = "",
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val squareFootage: Int = 0,
    val gardenSize: Int = 0,
    val propertyType: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val features: List<String> = emptyList(),
    val agentId: String = "",
    val agentName: String = "",
    val agentPhone: String = "",
    val agentEmail: String = "",
    val yearBuilt: Int = 0,
    val createdAt: Long = 0,
    val isLiked: Boolean = false, // Changed from var to val
    val isSwiped: Boolean = false // Changed from var to val
)

data class HousesResponse(
    val houses: List<House> = emptyList(),
    val metadata: Metadata = Metadata()
)

data class Metadata(
    val total: Int = 0,
    val lastUpdated: Long = 0,
    val version: String = ""
)
