package com.example.osso.repositories



import com.example.osso.models.House
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HouseRepository {
    private val database = FirebaseDatabase.getInstance("https://hometinder-d4409-default-rtdb.europe-west1.firebasedatabase.app/")
    private val housesRef = database.getReference("houses")
    private val likedHousesRef = database.getReference("liked_houses")

    fun getHouses(): Flow<List<House>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val houses = mutableListOf<House>()
                snapshot.children.forEach { child ->
                    child.getValue(House::class.java)?.let { house ->
                        houses.add(house.copy(id = child.key ?: ""))
                    }
                }
                trySend(houses)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        housesRef.addValueEventListener(listener)
        awaitClose { housesRef.removeEventListener(listener) }
    }

    suspend fun likeHouse(houseId: String, userId: String) {
        likedHousesRef.child(userId).child(houseId).setValue(true).await()
        housesRef.child(houseId).child("isLiked").setValue(true).await()
    }

    suspend fun dislikeHouse(houseId: String, userId: String) {
        likedHousesRef.child(userId).child(houseId).removeValue().await()
        housesRef.child(houseId).child("isLiked").setValue(false).await()
    }

    fun getLikedHouses(userId: String): Flow<List<House>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likedHouseIds = snapshot.children.map { it.key ?: "" }

                housesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(housesSnapshot: DataSnapshot) {
                        val likedHouses = mutableListOf<House>()
                        housesSnapshot.children.forEach { child ->
                            val house = child.getValue(House::class.java)
                            if (house != null && likedHouseIds.contains(child.key)) {
                                likedHouses.add(house.copy(id = child.key ?: ""))
                            }
                        }
                        trySend(likedHouses)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        likedHousesRef.child(userId).addValueEventListener(listener)
        awaitClose { likedHousesRef.child(userId).removeEventListener(listener) }
    }
}