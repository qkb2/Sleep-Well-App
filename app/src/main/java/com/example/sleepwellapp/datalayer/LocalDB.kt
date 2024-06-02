package com.example.sleepwellapp.datalayer

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import java.util.Date

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

@Entity(tableName = "motion_count")
data class MotionCount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val day: String,
    val count: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MotionCountDao {
    @Query("SELECT * FROM motion_count WHERE day = :day ORDER BY timestamp DESC LIMIT 1")
    fun getLastCountForDay(day: String): MotionCount?

    @Insert
    fun insert(motionCount: MotionCount)

    @Query("UPDATE motion_count SET count = :count WHERE id = :id")
    fun updateCount(id: Int, count: Int)

    @Query("DELETE FROM motion_count WHERE timestamp < :cutoffTs")
    fun deleteOldData(cutoffTs: Long)
}


@Database(entities = [DayTimeEntity::class, MotionCount::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayTimeDao(): DayTimeDao
    abstract fun motionCountDao(): MotionCountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app-db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}