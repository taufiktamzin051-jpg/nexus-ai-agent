package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.AgentDatabase
import com.example.data.local.ConversationSummary
import com.example.data.model.AgentSettings
import com.example.data.model.ConversationLog
import com.example.data.model.Product
import com.example.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgentRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AgentDatabase::class.java,
        "ai_agent_business.db"
    ).build()

    private val productDao = db.productDao()
    private val transactionDao = db.transactionDao()
    private val conversationLogDao = db.conversationLogDao()
    private val agentSettingsDao = db.agentSettingsDao()

    init {
        // Initialize default products and settings asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            checkAndSeedData()
        }
    }

    // --- Product DAO wrappers ---
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()
    suspend fun getProductById(id: Int): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }
    suspend fun insertProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }
    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }
    suspend fun deleteProductById(id: Int) = withContext(Dispatchers.IO) {
        productDao.deleteProductById(id)
    }

    // --- Transaction DAO wrappers ---
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    suspend fun insertTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }
    fun getTotalEarnings(): Flow<Double?> = transactionDao.getTotalEarnings()

    // --- Conversation Log DAO wrappers ---
    fun getAllConversationLogs(): Flow<List<ConversationLog>> = conversationLogDao.getAllConversationLogs()
    fun getLogsForConversation(convoId: String): Flow<List<ConversationLog>> = conversationLogDao.getLogsForConversation(convoId)
    fun getDistinctConversations(): Flow<List<ConversationSummary>> = conversationLogDao.getDistinctConversations()
    suspend fun insertLog(log: ConversationLog) = withContext(Dispatchers.IO) {
        conversationLogDao.insertLog(log)
    }
    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        conversationLogDao.clearAllLogs()
    }

    // --- Agent Settings DAO wrappers ---
    fun getSettingsFlow(): Flow<AgentSettings?> = agentSettingsDao.getSettingsFlow()
    suspend fun getSettings(): AgentSettings = withContext(Dispatchers.IO) {
        agentSettingsDao.getSettings() ?: AgentSettings().also {
            agentSettingsDao.insertOrUpdateSettings(it)
        }
    }
    suspend fun updateSettings(settings: AgentSettings) = withContext(Dispatchers.IO) {
        agentSettingsDao.insertOrUpdateSettings(settings)
    }

    // --- Database Seeding ---
    private suspend fun checkAndSeedData() {
        // Seed Products if none exist
        val products = productDao.getAllProducts().first()
        if (products.isEmpty()) {
            val defaultProducts = listOf(
                Product(
                    name = "E-Book: Dropship AI Mastery",
                    category = "E-Book",
                    price = 49000.0,
                    stock = 50,
                    description = "Buku panduan lengkap taktik dropship produk luar negeri memanfaatkan kecerdasan buatan untuk otomatisasi riset pasar, pembuatan copywriting, dan optimasi iklan sosial media.",
                    autoDeliveryContent = "Terimakasih atas pembelian Anda! Hubungi tim bantuan jika ada kendala. Download buku digital Anda di sini: https://bit.ly/dropship-ai-mastery-ebook"
                ),
                Product(
                    name = "Windows 11 Pro Genuine Retail License",
                    category = "License Key",
                    price = 75000.0,
                    stock = 15,
                    description = "Lisensi Windows 11 Professional Retail resmi, berlaku seumur hidup (Lifetime) untuk 1 PC. Aktivasi langsung secara online via Server Microsoft resmi.",
                    autoDeliveryContent = "Key Anda: W269N-WFGWX-YVC9B-4J6C9-T83GX. Cara aktivasi: Buka Settings -> Activation -> Change Product Key -> Klik Next -> Sukses!"
                ),
                Product(
                    name = "Canva Pro Premium 1-Tahun Undangan",
                    category = "Voucher",
                    price = 29000.0,
                    stock = 30,
                    description = "Akses penuh ke semua fitur Canva Pro (Template, Font premium, Background remover, Brand kit, dlsb) menggunakan akun pribadi Anda melalui undangan tim berlisensi.",
                    autoDeliveryContent = "Terimakasih telah membeli! Undangan Tim Canva Pro Anda aktif. Klik undangan tim Anda di sini: https://canva.com/brand/join?invite=CANVA-PRO-1Y-TEAM-AISTUDIO"
                ),
                Product(
                    name = "ChatGPT Plus Premium Sharing Account 30 Hari",
                    category = "Premium Account",
                    price = 35000.0,
                    stock = 8,
                    description = "Akun bersama ChatGPT Plus (GPT-4o, GPT-4, DALL-E 3) dengan akses penuh selama 30 hari. Sangat hemat dan cocok untuk pengerjaan tugas, riset, atau copywriter harian.",
                    autoDeliveryContent = "Kredensial Login ChatGPT Plus Sharing:\nEmail: chatgptsharing12@gmaster.com\nPassword: GPT-4o-Access-Secure!\nCatatan: Dilarang mengubah email/password atau menghapus riwayat chat pengguna lain."
                )
            )
            for (p in defaultProducts) {
                productDao.insertProduct(p)
            }
        }

        // Seed settings if empty
        val currentSettings = agentSettingsDao.getSettings()
        if (currentSettings == null) {
            agentSettingsDao.insertOrUpdateSettings(AgentSettings())
        }
    }
}
