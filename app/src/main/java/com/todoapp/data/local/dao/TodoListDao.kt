package com.todoapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.todoapp.data.local.entity.TodoListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoListDao {
    @Query("SELECT * FROM lists ORDER BY type ASC, name ASC")
    fun getAllLists(): Flow<List<TodoListEntity>>

    @Query("SELECT * FROM lists ORDER BY type ASC, name ASC")
    suspend fun getAllListsList(): List<TodoListEntity>

    @Query("SELECT * FROM lists WHERE id = '00000000-0000-0000-0000-000000000001' LIMIT 1")
    suspend fun getPublicList(): TodoListEntity?

    @Query("SELECT * FROM lists WHERE id = '00000000-0000-0000-0000-000000000001' LIMIT 1")
    fun getPublicListFlow(): Flow<TodoListEntity?>

    @Query("SELECT * FROM lists WHERE type = 'private' AND ownerId = :ownerId ORDER BY name ASC")
    fun getPrivateListsByOwner(ownerId: String): Flow<List<TodoListEntity>>

    @Query("SELECT * FROM lists WHERE id = :listId")
    suspend fun getListById(listId: String): TodoListEntity?

    @Query("SELECT * FROM lists WHERE id = :listId")
    fun getListByIdFlow(listId: String): Flow<TodoListEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: TodoListEntity)

    @Update
    suspend fun updateList(list: TodoListEntity)

    @Delete
    suspend fun deleteList(list: TodoListEntity)

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteListById(listId: String)
}
