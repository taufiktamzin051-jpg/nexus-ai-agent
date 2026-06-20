package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // e.g. "Voucher", "E-Book", "Premium Account", "License Key"
    val price: Double,
    val stock: Int,
    val description: String,
    val autoDeliveryContent: String, // License key, download link, account detail, etc.
    val soldCount: Int = 0
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val customerEmail: String,
    val productId: Int,
    val productName: String,
    val pricePaid: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS", "PENDING", "FAILED"
    val autoDeliveredItem: String, // The license code or link sent to user
    val chatbotChatHistoryId: String // Links to conversation logs
)

@Entity(tableName = "conversation_logs")
data class ConversationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: String, // Unique UUID for customer conversation
    val customerName: String,
    val sender: String, // "CUSTOMER", "AGENT" or "SYSTEM_AUTODELIVERY"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val thoughtLog: String? = null // Hidden agent rationale "Thinking..."
)

@Entity(tableName = "agent_settings")
data class AgentSettings(
    @PrimaryKey val id: Int = 1,
    val agentName: String = "Sora AI",
    val tone: String = "Friendly", // "Friendly", "Professional", "Hustler", "Technical"
    val customRules: String = "Selalu berikan diskon 5% jika pelanggan ragu. Pastikan pelanggan merasa terbantu dan beritahu stok sisa produk secara akurat.",
    val isAutoReplyEnabled: Boolean = true,
    val businessObjective: String = "Meningkatkan penjualan produk digital demi ROI maksimal konsumen."
)

data class PotentialClient(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val profession: String,
    val interest: String,
    val background: String,
    val pitchStrategy: String
)

