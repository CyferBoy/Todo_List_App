package com.todoapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.data.local.entity.DeletedListEntity

@Dao
interface DeletedListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedList(deletedList: DeletedListEntity)

    @Query("SELECT * FROM deleted_lists WHERE id = :listId LIMIT 1")
    suspend fun getDeletedListById(listId: String): DeletedListEntity?

    @Query("SELECT * FROM deleted_lists WHERE deletedAt > :since")
    suspend fun getDeletedListsSince(since: Long): List<DeletedListEntity>

    @Query("DELETE FROM deleted_lists WHERE deletedAt < :before")
    suspend fun cleanOldDeletedLists(before: Long)

    @Query("DELETE FROM deleted_lists WHERE id = :listId")
    suspend fun deleteByListId(listId: String)
}
