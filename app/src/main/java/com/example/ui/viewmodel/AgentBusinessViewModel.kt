package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.model.AgentSettings
import com.example.data.model.ConversationLog
import com.example.data.model.Product
import com.example.data.model.Transaction
import com.example.data.model.PotentialClient
import com.example.data.repository.AgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AgentBusinessViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AgentRepository(application)

    // --- Database Source Flows ---
    val productsList: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactionsList: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalEarnings: StateFlow<Double?> = repository.getTotalEarnings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val agentSettings: StateFlow<AgentSettings?> = repository.getSettingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // --- UI Local States ---
    private val _isGeneratingResponse = MutableStateFlow(false)
    val isGeneratingResponse: StateFlow<Boolean> = _isGeneratingResponse.asStateFlow()

    private val _agentThoughtLog = MutableStateFlow<String?>(null)
    val agentThoughtLog: StateFlow<String?> = _agentThoughtLog.asStateFlow()

    private val _currentConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ConversationLog>>(emptyList())
    val chatMessages: StateFlow<List<ConversationLog>> = _chatMessages.asStateFlow()

    // --- Active Customer Simulator Context ---
    private val _activeCustomerName = MutableStateFlow("Budi Santoso")
    val activeCustomerName: StateFlow<String> = _activeCustomerName.asStateFlow()

    // --- Promo & Client Hunter States ---
    private val _targetPotentialClients = MutableStateFlow<List<PotentialClient>>(emptyList())
    val targetPotentialClients: StateFlow<List<PotentialClient>> = _targetPotentialClients.asStateFlow()

    private val _isSearchingClients = MutableStateFlow(false)
    val isSearchingClients: StateFlow<Boolean> = _isSearchingClients.asStateFlow()

    private val _isGeneratingPromoState = MutableStateFlow(false)
    val isGeneratingPromoState: StateFlow<Boolean> = _isGeneratingPromoState.asStateFlow()


    init {
        // Observe conversation logs
        viewModelScope.launch {
            repository.getAllConversationLogs().collect { logs ->
                // Filter current convo logs locally for high responsiveness
                _chatMessages.value = logs.filter { it.conversationId == _currentConversationId.value }
            }
        }
    }

    /**
     * Updates active simulation customer
     */
    fun selectCustomer(name: String) {
        _activeCustomerName.value = name
        // Reset conversation ID for a brand-new customer chat session
        val newId = UUID.randomUUID().toString()
        _currentConversationId.value = newId
        _agentThoughtLog.value = "Sesi baru dimulai dengan pelanggan: $name."
        _chatMessages.value = emptyList()
    }

    /**
     * Sends message from customer in simulation, running automated agent loop
     */
    fun sendCustomerMessage(text: String) {
        if (text.isBlank()) return
        val convoId = _currentConversationId.value
        val customerName = _activeCustomerName.value

        viewModelScope.launch {
            // 1. Log the customer text
            val customerLog = ConversationLog(
                conversationId = convoId,
                customerName = customerName,
                sender = "CUSTOMER",
                message = text
            )
            repository.insertLog(customerLog)

            _isGeneratingResponse.value = true
            _agentThoughtLog.value = "Asisten sedang menganalisa pesan: '$text'..."

            // 2. Fetch inventory and settings to pass to AI context
            val currentProducts = productsList.value
            val currentSettings = agentSettings.value ?: AgentSettings()

            // Build historical list of Pair(sender, text) in current chat
            val history = chatMessages.value.map { Pair(it.sender, it.message) }

            // 3. Request AI Output
            val (aiResponse, thoughts) = GeminiClient.getAgentResponse(
                customerMessage = text,
                conversationHistory = history,
                products = currentProducts,
                settings = currentSettings
            )
            
            _agentThoughtLog.value = thoughts

            // 4. Process response actions (e.g. BUY check)
            var cleanResponse = aiResponse
            val buyActionRegex = Regex("\\[ACTION:BUY_PRODUCT_ID:(\\d+)\\]")
            val match = buyActionRegex.find(aiResponse)

            if (match != null) {
                // Customer wants to buy a product!
                val productId = match.groupValues[1].toIntOrNull()
                cleanResponse = aiResponse.replace(buyActionRegex, "").trim()

                // Insert agent speech first
                val agentSpeechLog = ConversationLog(
                    conversationId = convoId,
                    customerName = customerName,
                    sender = "AGENT",
                    message = cleanResponse,
                    thoughtLog = thoughts
                )
                repository.insertLog(agentSpeechLog)

                if (productId != null) {
                    val product = repository.getProductById(productId)
                    if (product != null) {
                        if (product.stock > 0) {
                            // Process Successful Transaction (Auto Pilot Delivery!)
                            val updatedProduct = product.copy(
                                stock = product.stock - 1,
                                soldCount = product.soldCount + 1
                            )
                            repository.updateProduct(updatedProduct)

                            // Save successful transaction
                            val newTx = Transaction(
                                customerName = customerName,
                                customerEmail = "${customerName.lowercase().replace(" ", "")}@domain.com",
                                productId = product.id,
                                productName = product.name,
                                pricePaid = product.price,
                                status = "SUCCESS",
                                autoDeliveredItem = product.autoDeliveryContent,
                                chatbotChatHistoryId = convoId
                            )
                            repository.insertTransaction(newTx)

                            // Save system automated delivery log
                            val sysLog = ConversationLog(
                                conversationId = convoId,
                                customerName = customerName,
                                sender = "SYSTEM_AUTODELIVERY",
                                message = "🚀 AUTOPILOT DELIVERY:\nProduk digital '${product.name}' berhasil dikirim ke pelanggan!\n\n📋 ISI PRODUK:\n${product.autoDeliveryContent}"
                            )
                            repository.insertLog(sysLog)
                            _agentThoughtLog.value = "${thoughts}\n\n[SISTEM] TRANSAKSI BERHASIL: Stock berkurang, lisensi terkirim secara instan!"
                        } else {
                            // Out of stock
                            val sysLog = ConversationLog(
                                conversationId = convoId,
                                customerName = customerName,
                                sender = "AGENT",
                                message = "Maaf kak, kelihatannya stok untuk produk '${product.name}' baru saja habis. Transaksi dihentikan otomatis."
                            )
                            repository.insertLog(sysLog)
                        }
                    }
                }
            } else {
                // Ordinary question / customer interaction
                val agentLog = ConversationLog(
                    conversationId = convoId,
                    customerName = customerName,
                    sender = "AGENT",
                    message = cleanResponse,
                    thoughtLog = thoughts
                )
                repository.insertLog(agentLog)
            }

            _isGeneratingResponse.value = false
        }
    }

    // --- Product Management wrappers ---
    fun addProduct(name: String, category: String, price: Double, stock: Int, description: String, deliveryContent: String) {
        viewModelScope.launch {
            repository.insertProduct(
                Product(
                    name = name,
                    category = category,
                    price = price,
                    stock = stock,
                    description = description,
                    autoDeliveryContent = deliveryContent
                )
            )
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            repository.deleteProductById(id)
        }
    }

    // --- Agent Actions wrappers ---
    fun updateSettings(settings: AgentSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun clearLogsAndReset() {
        viewModelScope.launch {
            repository.clearAllLogs()
            // Reset state
            val newId = UUID.randomUUID().toString()
            _currentConversationId.value = newId
            _chatMessages.value = emptyList()
            _agentThoughtLog.value = "Database log simulasi dibersihkan. Sesi autopilot siap sedia."
        }
    }

    // --- PROMO & PROSPECT FINDER IMPLEMENTATION (NEW) ---

    fun searchClientsForProduct(productName: String, targetNiche: String) {
        viewModelScope.launch {
            _isSearchingClients.value = true
            try {
                val foundList = GeminiClient.generatePotentialClients(productName, targetNiche)
                _targetPotentialClients.value = foundList
            } catch (e: Exception) {
                // Return fallback empty or mocked
                _targetPotentialClients.value = emptyList()
            } finally {
                _isSearchingClients.value = false
            }
        }
    }

    suspend fun generatePromoMsg(productName: String, productDesc: String, channel: String): String {
        _isGeneratingPromoState.value = true
        return try {
            GeminiClient.generatePromoPlan(productName, productDesc, channel)
        } catch (e: Exception) {
            "Gagal memprogres naskah promosi: ${e.localizedMessage}"
        } finally {
            _isGeneratingPromoState.value = false
        }
    }

    fun pitchAndConvertClient(client: PotentialClient, product: Product, customizedPitch: String) {
        val convoId = UUID.randomUUID().toString()
        _currentConversationId.value = convoId
        _activeCustomerName.value = client.name
        _chatMessages.value = emptyList()

        viewModelScope.launch {
            _isGeneratingResponse.value = true
            _agentThoughtLog.value = "[AI CAMPAIGN PITCH]\n" +
                    "Menghubungi Calon Pembeli: ${client.name}\n" +
                    "Niche Bisnis: ${client.profession}\n" +
                    "Produk Ditawarkan: ${product.name}\n" +
                    "Strategi Lead Playbook: ${client.pitchStrategy}\n" +
                    "Mengirim copywriting persuasif..."

            // 1. Log the Agent's pitch
            val pitchLog = ConversationLog(
                conversationId = convoId,
                customerName = client.name,
                sender = "AGENT",
                message = customizedPitch,
                thoughtLog = "Copywriting promo berhasil disiarkan secara personal ke klien."
            )
            repository.insertLog(pitchLog)

            kotlinx.coroutines.delay(1800) // Realistic reading gap

            // 2. Client converts enthusiastically! Log positive reply.
            val interestReply = "Halo kak, terima kasih penawarannya! Kebetulan sekali saya sebagai desainer/pemilik ${client.profession} memang sedang menghadapi masalah: ${client.background}.\n" +
                    "Solusi dari '${product.name}' ini cocok banget! Saya mau langsung order 1 unit sekarang ya kak."
            val customerLog = ConversationLog(
                conversationId = convoId,
                customerName = client.name,
                sender = "CUSTOMER",
                message = interestReply
            )
            repository.insertLog(customerLog)

            _agentThoughtLog.value = "Klien ${client.name} berkonversi menjadi PEMBELI!\nMemproses checkout tagihan..."
            kotlinx.coroutines.delay(1200)

            // 3. Process database updates & deliver digital goods
            val freshProduct = repository.getProductById(product.id) ?: product
            if (freshProduct.stock > 0) {
                val updatedProduct = freshProduct.copy(
                    stock = freshProduct.stock - 1,
                    soldCount = freshProduct.soldCount + 1
                )
                repository.updateProduct(updatedProduct)

                val newTx = Transaction(
                    customerName = client.name,
                    customerEmail = "${client.name.lowercase().replace(" ", "")}@domain.com",
                    productId = freshProduct.id,
                    productName = freshProduct.name,
                    pricePaid = freshProduct.price,
                    status = "SUCCESS",
                    autoDeliveredItem = freshProduct.autoDeliveryContent,
                    chatbotChatHistoryId = convoId
                )
                repository.insertTransaction(newTx)

                val sysLog = ConversationLog(
                    conversationId = convoId,
                    customerName = client.name,
                    sender = "SYSTEM_AUTODELIVERY",
                    message = "🚀 AUTOPILOT DELIVERY:\nProduk digital '${freshProduct.name}' berhasil dikirim ke pelanggan premium baru!\n\n📋 ISI PRODUK:\n${freshProduct.autoDeliveryContent}"
                )
                repository.insertLog(sysLog)
                
                _agentThoughtLog.value = "[KONVERSI SUKSES]\nAutopilot berhasil menutup penjualan (deal)!\n" +
                        "Aset lisensi digital telah otomatis terkirim untuk ${client.name}!"
            } else {
                val outOfStockLog = ConversationLog(
                    conversationId = convoId,
                    customerName = client.name,
                    sender = "SYSTEM_AUTODELIVERY",
                    message = "Maaf sekali ${client.name}, stok produk '${freshProduct.name}' baru saja habis. Transaksi dibatalkan."
                )
                repository.insertLog(outOfStockLog)
            }

            _isGeneratingResponse.value = false
        }
    }
}

