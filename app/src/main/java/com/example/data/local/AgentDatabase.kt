package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.AgentSettings
import com.example.data.model.ConversationLog
import com.example.data.model.Product
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT SUM(pricePaid) FROM transactions WHERE status = 'SUCCESS'")
    fun getTotalEarnings(): Flow<Double?>
}

@Dao
interface ConversationLogDao {
    @Query("SELECT * FROM conversation_logs ORDER BY timestamp ASC")
    fun getAllConversationLogs(): Flow<List<ConversationLog>>

    @Query("SELECT * FROM conversation_logs WHERE conversationId = :convoId ORDER BY timestamp ASC")
    fun getLogsForConversation(convoId: String): Flow<List<ConversationLog>>

    @Query("SELECT DISTINCT conversationId, customerName FROM conversation_logs")
    fun getDistinctConversations(): Flow<List<ConversationSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConversationLog)

    @Query("DELETE FROM conversation_logs")
    suspend fun clearAllLogs()
}

data class ConversationSummary(
    val conversationId: String,
    val customerName: String
)

@Dao
interface AgentSettingsDao {
    @Query("SELECT * FROM agent_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AgentSettings?>

    @Query("SELECT * FROM agent_settings WHERE id = 1")
    suspend fun getSettings(): AgentSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AgentSettings)
}

@Database(
    entities = [Product::class, Transaction::class, ConversationLog::class, AgentSettings::class],
    version = 1,
    exportSchema = false
)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun conversationLogDao(): ConversationLogDao
    abstract fun agentSettingsDao(): AgentSettingsDao
}
