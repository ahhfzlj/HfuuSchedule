package com.hfuu.schedule.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hfuu.schedule.data.model.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY dayOfWeek ASC, startSection ASC")
    fun observeAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<CourseEntity>)

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(courses: List<CourseEntity>) {
        deleteAll()
        insertAll(courses)
    }
}
