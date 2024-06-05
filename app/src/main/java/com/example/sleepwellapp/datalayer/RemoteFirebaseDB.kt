package com.example.sleepwellapp.datalayer


import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


data class RemoteDayTimeEntity(
    val userId: String = "",
    val id: Int = 0,
    val day: String = "",
    val wakeUpTime: String = "",
    val sleepTime: String = "",
    val enforced: Boolean = false,
    var documentId: String = "",
)

fun toLocalDbFormat(remoteDayTimeEntity: RemoteDayTimeEntity): DayTimeEntity {
    return DayTimeEntity(
        id = remoteDayTimeEntity.id,
        day = remoteDayTimeEntity.day,
        wakeUpTime = remoteDayTimeEntity.wakeUpTime,
        sleepTime = remoteDayTimeEntity.sleepTime,
        enforced = remoteDayTimeEntity.enforced
    )
}

fun toRemoteDbFormat(dayTimeEntity: DayTimeEntity, userId: String): RemoteDayTimeEntity {
    return RemoteDayTimeEntity(
        userId = userId,
        id = dayTimeEntity.id,
        day = dayTimeEntity.day,
        wakeUpTime = dayTimeEntity.wakeUpTime,
        sleepTime = dayTimeEntity.sleepTime,
        enforced = dayTimeEntity.enforced
    )
}

const val NOTES_COLLECTION_REF = "dayTimes"
class RemoteFirebaseDB {

    private val firestore = FirebaseFirestore.getInstance()
    fun user() = Firebase.auth.currentUser

    suspend fun fetchAllDayTimes(): List<RemoteDayTimeEntity> {
        val user = user()
        return if (user != null) {
            val userId = user.uid
            try {
                val result = firestore.collection("dayTimes")
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
            firestore.collection("dayTimes")
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
            firestore.collection("dayTimes")
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
            firestore.collection("dayTimes")
                .document(documentId)
                .set(updatedDayTime)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

}


























