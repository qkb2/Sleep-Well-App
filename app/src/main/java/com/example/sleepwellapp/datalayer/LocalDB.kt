package com.example.sleepwellapp.datalayer

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(tableName = "day_times")
data class DayTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val day: String,
    val wakeUpTime: String,
    val sleepTime: String,
    val enforced: Boolean = false
)

@Dao
interface DayTimeDao {
    @Query("SELECT * FROM day_times")
    fun getAll(): List<DayTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dayTime: DayTimeEntity)

    @Update
    fun update(dayTime: DayTimeEntity)

    @Delete
    fun delete(dayTime: DayTimeEntity)

    @Query("DELETE FROM day_times")
    fun deleteAll()

    @Query("SELECT * FROM day_times WHERE day = :day LIMIT 1")
    fun getByDay(day: String): DayTimeEntity?
}

@Database(entities = [DayTimeEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayTimeDao(): DayTimeDao
}