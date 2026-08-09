package com.cryptosafe.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class BoxWithCount(
    @Embedded val box: Box,
    val messageCount: Int
)

@Dao
interface BoxDao {
    @Query("SELECT * FROM boxes ORDER BY createdAt DESC")
    fun getAllBoxes(): Flow<List<Box>>

    @Query(
        "SELECT boxes.*, " +
            "(SELECT COUNT(*) FROM messages WHERE messages.boxId = boxes.id) AS messageCount " +
            "FROM boxes ORDER BY createdAt DESC"
    )
    fun getAllBoxesWithCount(): Flow<List<BoxWithCount>>

    @Query("SELECT * FROM boxes ORDER BY createdAt DESC")
    suspend fun getAllBoxesSync(): List<Box>

    @Query("SELECT * FROM boxes WHERE id = :boxId")
    fun getBoxById(boxId: Long): Flow<Box?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBox(box: Box): Long

    @Update
    suspend fun updateBox(box: Box)

    @Delete
    suspend fun deleteBox(box: Box)

    @Query("DELETE FROM boxes WHERE id = :boxId")
    suspend fun deleteBoxById(boxId: Long)

    @Query("SELECT * FROM messages WHERE boxId = :boxId ORDER BY timestamp ASC")
    fun getMessagesByBoxId(boxId: Long): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Query("SELECT * FROM messages WHERE boxId = :boxId ORDER BY timestamp ASC")
    suspend fun getMessagesByBoxIdSync(boxId: Long): List<Message>

    @Query("DELETE FROM messages WHERE boxId = :boxId")
    suspend fun deleteMessagesByBoxId(boxId: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE boxId = :boxId")
    fun getMessageCount(boxId: Long): Flow<Int>

    @Query("SELECT * FROM messages WHERE boxId = :boxId AND timestamp < :beforeTimestamp")
    suspend fun getMessagesBefore(boxId: Long, beforeTimestamp: Long): List<Message>

    @Query("DELETE FROM messages WHERE boxId = :boxId AND timestamp < :beforeTimestamp")
    suspend fun deleteMessagesBefore(boxId: Long, beforeTimestamp: Long)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)
}
