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
data class NightTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDay: String,
    val endDay: String,
    val sleepTime: String,
    val wakeUpTime: String,
    val enabled: Boolean
)

@Dao
interface NightTimeDao {
    @Query("SELECT * FROM day_times")
    fun getAll(): List<NightTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dayTime: NightTimeEntity)

    @Update
    fun update(dayTime: NightTimeEntity)

    @Delete
    fun delete(dayTime: NightTimeEntity)

    @Query("DELETE FROM day_times")
    fun deleteAll()

    @Query("SELECT * FROM day_times WHERE startDay = :day LIMIT 1")
    fun getByStartDay(day: String): NightTimeEntity?
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


@Database(entities = [NightTimeEntity::class, MotionCount::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nightTimeDao(): NightTimeDao
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