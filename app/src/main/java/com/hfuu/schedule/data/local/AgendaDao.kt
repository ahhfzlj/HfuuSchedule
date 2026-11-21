package com.hfuu.schedule.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hfuu.schedule.data.model.AgendaEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaDao {
    @Query("SELECT * FROM agenda_events ORDER BY isCompleted ASC, startMillis ASC")
    fun observeAllEvents(): Flow<List<AgendaEventEntity>>

    @Query("SELECT * FROM agenda_events WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): AgendaEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AgendaEventEntity): Long

    @Update
    suspend fun update(event: AgendaEventEntity)

    @Delete
    suspend fun delete(event: AgendaEventEntity)

    @Query("UPDATE agenda_events SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM agenda_events")
    suspend fun deleteAll()
}
