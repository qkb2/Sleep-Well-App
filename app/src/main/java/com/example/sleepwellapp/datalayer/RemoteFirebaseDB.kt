package com.example.sleepwellapp.datalayer


import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await


data class RemoteDayTimeEntity(
    val userId: String = "",
    val id: Int = 0,
    var documentId: String = "",
    val startDay: String,
    val endDay: String,
    val sleepTime: String,
    val wakeUpTime: String,
    val enabled: Boolean
)

fun toLocalDbFormat(remoteDayTimeEntity: RemoteDayTimeEntity): NightTimeEntity {
    return NightTimeEntity(
        remoteDayTimeEntity.id,
        remoteDayTimeEntity.startDay,
        remoteDayTimeEntity.endDay,
        remoteDayTimeEntity.sleepTime,
        remoteDayTimeEntity.wakeUpTime,
        remoteDayTimeEntity.enabled
    )
}

fun toRemoteDbFormat(nightTimeEntity: NightTimeEntity, userId: String): RemoteDayTimeEntity {
    return RemoteDayTimeEntity(
        userId = userId,
        id = nightTimeEntity.id,
        startDay = nightTimeEntity.startDay,
        endDay = nightTimeEntity.endDay,
        sleepTime = nightTimeEntity.sleepTime,
        wakeUpTime = nightTimeEntity.wakeUpTime,
        enabled = nightTimeEntity.enabled
    )
}

const val NIGHT_COLLECTION_REF = "nightTimes"
class RemoteFirebaseDB {

    private val firestore = FirebaseFirestore.getInstance()
    fun user() = Firebase.auth.currentUser

    suspend fun fetchAllDayTimes(): List<RemoteDayTimeEntity> {
        val user = user()
        return if (user != null) {
            val userId = user.uid
            try {
                val result = firestore.collection(NIGHT_COLLECTION_REF)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                result.documents.map { document ->
                    document.toObject(RemoteDayTimeEntity::class.java)!!.copy(documentId = document.id)
                }
            } catch (e: Exception) {
                Log.d("TAG", "fetchAllDayTimes: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun addDayTime(dayTime: RemoteDayTimeEntity): Boolean {
        val userId = dayTime.userId
        val dayTimeWithUserId = dayTime.copy(userId = userId)
        return try {
            firestore.collection(NIGHT_COLLECTION_REF)
                .add(dayTimeWithUserId)
                .addOnSuccessListener { documentReference ->
                    Log.d("ADD TAG", "DocumentSnapshot written with ID: ${documentReference.id}")
                    dayTime.documentId = documentReference.id
                }
                .addOnFailureListener { e ->
                    Log.w("ADD TAG", "Error adding document", e)
                }

            Log.d("TAG", "addDayTime: added dayTime")
            true
        } catch (e: Exception) {
            Log.d("TAG", "addDayTime: ${e.message}")
            false
        }
    }

    suspend fun deleteDayTime(documentId: String): Boolean {
        return try {
            firestore.collection(NIGHT_COLLECTION_REF)
                .document(documentId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e("TAG", "deleteDayTime: ${e.message}", e)
            false
        }
    }

    suspend fun updateDayTime(documentId: String, updatedDayTime: RemoteDayTimeEntity): Boolean {
        return try {
            val userId = updatedDayTime.userId
            firestore.collection(NIGHT_COLLECTION_REF)
                .document(documentId)
                .set(updatedDayTime)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

}


























