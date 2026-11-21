package com.hfuu.schedule.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hfuu.schedule.data.model.ScheduleMemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query("SELECT * FROM schedule_memos ORDER BY dayOfWeek ASC, startSection ASC, id ASC")
    fun observeAllMemos(): Flow<List<ScheduleMemoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memo: ScheduleMemoEntity): Long

    @Update
    suspend fun update(memo: ScheduleMemoEntity)

    @Delete
    suspend fun delete(memo: ScheduleMemoEntity)

    @Query("UPDATE schedule_memos SET isDone = :done WHERE id = :id")
    suspend fun updateDone(id: Long, done: Boolean)

    @Query("DELETE FROM schedule_memos")
    suspend fun deleteAll()
}
