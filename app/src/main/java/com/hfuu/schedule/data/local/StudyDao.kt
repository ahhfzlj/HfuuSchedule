package com.hfuu.schedule.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hfuu.schedule.data.model.StudyTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_tasks ORDER BY isCompleted ASC, CASE WHEN deadlineMillis > 0 THEN 0 ELSE 1 END ASC, deadlineMillis ASC")
    fun observeAllTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT COUNT(*) FROM study_tasks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<StudyTaskEntity>)

    @Query("UPDATE study_tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun updateCompleted(taskId: String, completed: Boolean)

    @Query("DELETE FROM study_tasks")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(tasks: List<StudyTaskEntity>) {
        deleteAll()
        insertAll(tasks)
    }
}
