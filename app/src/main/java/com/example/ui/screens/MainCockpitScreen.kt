package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.GeminiClient
import com.example.data.model.AgentSettings
import com.example.data.model.ConversationLog
import com.example.data.model.Product
import com.example.data.model.Transaction
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.AgentBusinessViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainCockpitScreen(
    viewModel: AgentBusinessViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- State collections from ViewModel ---
    val products by viewModel.productsList.collectAsState()
    val transactions by viewModel.transactionsList.collectAsState()
    val totalEarningsVal by viewModel.totalEarnings.collectAsState()
    val rawSettings by viewModel.agentSettings.collectAsState()
    val settings = rawSettings ?: AgentSettings()

    val isGenerating by viewModel.isGeneratingResponse.collectAsState()
    val thoughtLog by viewModel.agentThoughtLog.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val activeCustomerName by viewModel.activeCustomerName.collectAsState()

    // Local screen states
    var activeTab by remember { mutableStateOf(0) } // 0: Dasbor & Simulasi, 1: Produk, 2: Setelan
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showThoughtsPanel by remember { mutableStateOf(true) }

    val formattedEarnings = remember(totalEarningsVal) {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.format(totalEarningsVal ?: 0.0).replace("Rp", "Rp ")
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground),
        topBar = {
            // High-fidelity cockpit header control banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SlateSurface, SlateBackground)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(CyberPrimary.copy(alpha = 0.4f), CyberSecondary.copy(alpha = 0.1f))
                        ),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Agent Banner",
                                tint = CyberPrimary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("app_logo_icon")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "COCKPIT AUTOPILOT AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    color = TextLight,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        Text(
                            text = "Asisten Pemrosesan Bisnis Produk Digital Cerdas",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Status pill showing Autopilot configuration status
                    val keyConfigured = GeminiClient.isKeyConfigured()
                    val statusText = if (keyConfigured) "AUTOPILOT (GEMINI REST)" else "SIMULASI (SMART LOCAL)"
                    val statusColor = if (keyConfigured) SuccessGreen else CyberTertiary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Elegant M3 navigation bottom bar
            Surface(
                color = SlateSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val items = listOf(
                        Triple("Dasbor", Icons.Default.Chat, 0),
                        Triple("Produk", Icons.Default.Inventory, 1),
                        Triple("Promo & Klien", Icons.Default.Campaign, 2),
                        Triple("Setelan Agen", Icons.Default.Settings, 3)
                    )

                    items.forEach { (label, icon, tabIdx) ->
                        val selected = activeTab == tabIdx
                        val tintColor = if (selected) CyberPrimary else TextMuted

                        Column(
                            modifier = Modifier
                                .clickable { activeTab = tabIdx }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) CyberPrimary.copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = tintColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = tintColor,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // FAB to quickly add products when on product tab
            if (activeTab == 1) {
                FloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    containerColor = CyberPrimary,
                    contentColor = SlateBackground,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Produk Digital",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateBackground)
        ) {
            // Content according to active tab
            when (activeTab) {
                0 -> DashboardSimulasiView(
                    viewModel = viewModel,
                    products = products,
                    transactions = transactions,
                    formattedEarnings = formattedEarnings,
                    chatMessages = chatMessages,
                    isGenerating = isGenerating,
                    thoughtLog = thoughtLog,
                    activeCustomerName = activeCustomerName,
                    settings = settings,
                    showThoughtsPanel = showThoughtsPanel,
                    onToggleThoughts = { showThoughtsPanel = !showThoughtsPanel }
                )
                1 -> ProductManagementView(
                    products = products,
                    onEditProduct = { productToEdit = it },
                    onDeleteProduct = { viewModel.deleteProduct(it.id) }
                )
                2 -> PromoProspectView(
                    viewModel = viewModel,
                    products = products,
                    onGoToDashboard = { activeTab = 0 }
                )
                3 -> AgentSettingsView(
                    viewModel = viewModel,
                    settings = settings
                )
            }
        }
    }

    // --- DIALOGS ---

    // Add Product Dialog
    if (showAddProductDialog) {
        ProductFormDialog(
            title = "Tambah Produk Digital Baru",
            onSubmit = { name, cat, price, stock, desc, deliver ->
                viewModel.addProduct(name, cat, price, stock, desc, deliver)
                showAddProductDialog = false
            },
            onDismiss = { showAddProductDialog = false }
        )
    }

    // Edit Product Dialog
    productToEdit?.let { product ->
        ProductFormDialog(
            title = "Edit Produk Digital",
            product = product,
            onSubmit = { name, cat, price, stock, desc, deliver ->
                viewModel.updateProduct(
                    product.copy(
                        name = name,
                        category = cat,
                        price = price,
                        stock = stock,
                        description = desc,
                        autoDeliveryContent = deliver
                    )
                )
                productToEdit = null
            },
            onDismiss = { productToEdit = null }
        )
    }
}

// ==================== TAB 1: DASBOR & SIMULASI VIEW ====================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardSimulasiView(
    viewModel: AgentBusinessViewModel,
    products: List<Product>,
    transactions: List<Transaction>,
    formattedEarnings: String,
    chatMessages: List<ConversationLog>,
    isGenerating: Boolean,
    thoughtLog: String?,
    activeCustomerName: String,
    settings: AgentSettings,
    showThoughtsPanel: Boolean,
    onToggleThoughts: () -> Unit
) {
    val context = LocalContext.current
    var inputMessageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll down chats when new logs arrive or generation status changes
    LaunchedEffect(chatMessages.size, isGenerating) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // --- KPI Dash Cards ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Earning Card
            Card(
                modifier = Modifier.weight(1.2f),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = "Total Revenue",
                            tint = CyberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TOTAL REVENUE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = formattedEarnings,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }

            // Sales Count Card
            Card(
                modifier = Modifier.weight(0.9f),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberSecondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Total Sales",
                            tint = CyberSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TRANSAKSI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = "${transactions.size} Berhasil",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }

        // --- Simulated Customer Selector ---
        Text(
            text = "PILIH PELANGGAN SIMULASI (TRIGGER):",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextLight,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Ready-to-click customers with custom openers
        val mockCustomers = listOf(
            Triple("Budi Santoso", "Halo, apakah Windows 11 License Key ready? Cara install-nya gimana ya?", "Windows Key"),
            Triple("Siti Aminah", "Permisi kak, e-book dropship AI isinya apa aja ya? Dapat diskon nggak?", "E-Book"),
            Triple("Toni Wijaya", "Akun ChatGPT Plus Sharing-nya masih sisa stok satu? Saya mau beli dong kak.", "ChatGPT Account"),
            Triple("Rini Lestari", "Apakah bisa bantu kirim link canva premium sekarang juga kalau saya pesan?", "Canva Ticket")
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            mockCustomers.forEach { (name, opener, badge) ->
                val isActive = activeCustomerName == name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) CyberPrimary.copy(alpha = 0.15f) else SlateSurface)
                        .border(
                            1.dp,
                            if (isActive) CyberPrimary else Color(0xFF1E293B),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            viewModel.selectCustomer(name)
                            inputMessageText = opener
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isActive) CyberPrimary else TextLight,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Item: $badge",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 8.sp
                            )
                        )
                    }
                }
            }
        }

        // --- Split Panel: Conversation Scroll & Telemetry ---
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Chat Window
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .background(SlateSurface.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                // Chat header stating current participant
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateSurface)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chat Pelanggan: $activeCustomerName",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyberSecondary,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = { viewModel.clearLogsAndReset() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Bersihkan Log Chat",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Chat log
                Box(modifier = Modifier.weight(1f)) {
                    if (chatMessages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Tidak ada riwayat",
                                tint = TextMuted.copy(alpha = 0.3f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mulai Simulasi Pesanan",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Klik salah satu card pelanggan di atas untuk mengisi pesan awal bawaan, lalu tekan tombol kirim!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMessages) { log ->
                                ChatMessageBubble(log = log, context = context)
                            }

                            if (isGenerating) {
                                item {
                                    // Live thinking simulator block
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = CyberPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Asisten ${settings.agentName} sedang memprogres respon...",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = CyberPrimary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right expandable Telemetry/Thought Drawer (only on larger screen ratios or toggled)
            if (showThoughtsPanel) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .background(SlateSurface)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Rationale",
                                tint = CyberPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LOG AGENT SECURE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        IconButton(
                            onClick = onToggleThoughts,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Sembunyikan log",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(SlateBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = thoughtLog ?: "Menunggu interaksi masuk...\n" +
                                            "Asisten online. Inventory termonitor.\n" +
                                            "Semua instruksi sistem di-load sempurna.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CyberPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Message Input Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!showThoughtsPanel) {
                // Small toggle to show panel back
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurface)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .clickable { onToggleThoughts() }
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Tampilkan log berpikir",
                        tint = CyberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Input TextField
            TextField(
                value = inputMessageText,
                onValueChange = { inputMessageText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("customer_message_input"),
                placeholder = {
                    Text(
                        "Ketik pesan dari pembeli... (Misal: 'beli canva pro')",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SlateSurface,
                    unfocusedContainerColor = SlateSurface,
                    disabledContainerColor = SlateSurface,
                    focusedIndicatorColor = CyberPrimary,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputMessageText.isNotBlank() && !isGenerating) {
                            viewModel.sendCustomerMessage(inputMessageText)
                            inputMessageText = ""
                        }
                    }
                ),
                enabled = !isGenerating,
                maxLines = 2
            )

            // Submit Button
            IconButton(
                onClick = {
                    if (inputMessageText.isNotBlank() && !isGenerating) {
                        viewModel.sendCustomerMessage(inputMessageText)
                        inputMessageText = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (inputMessageText.isBlank() || isGenerating) SlateSurface else CyberPrimary)
                    .testTag("submit_message_button"),
                enabled = inputMessageText.isNotBlank() && !isGenerating
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Kirim pesan simulasi",
                    tint = if (inputMessageText.isBlank() || isGenerating) TextMuted else SlateBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(log: ConversationLog, context: Context) {
    val isCustomer = log.sender == "CUSTOMER"
    val isDelivery = log.sender == "SYSTEM_AUTODELIVERY"

    val alignment = if (isCustomer) Alignment.Start else Alignment.End
    val bgColor = when {
        isCustomer -> SlateSurfaceVariant
        isDelivery -> SuccessGreen.copy(alpha = 0.12f)
        else -> CyberPrimary.copy(alpha = 0.11f)
    }

    val borderColor = when {
        isCustomer -> Color(0xFF1E293B)
        isDelivery -> SuccessGreen
        else -> CyberPrimary.copy(alpha = 0.5f)
    }

    val bubbleShape = if (isCustomer) {
        RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        // Timestamp + Sender Label
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val senderLabel = when (log.sender) {
                "CUSTOMER" -> "Pelanggan"
                "SYSTEM_AUTODELIVERY" -> "🤖 AUTOPILOT DELIVERY"
                else -> "Asisten AI"
            }
            val tint = when (log.sender) {
                "CUSTOMER" -> TextMuted
                "SYSTEM_AUTODELIVERY" -> SuccessGreen
                else -> CyberPrimary
            }

            Text(
                text = senderLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = tint
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            Text(
                text = sdf.format(Date(log.timestamp)),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    color = TextMuted
                )
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Dialogue Box Card
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bgColor)
                .border(1.dp, borderColor, bubbleShape)
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDelivery) SuccessGreen else TextLight,
                        fontSize = 13.sp,
                        fontWeight = if (isDelivery) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 18.sp
                    )
                )

                // If delivery, show quick copy info card
                if (isDelivery) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SuccessGreen.copy(alpha = 0.15f))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("license_code", log.message.substringAfter("📋 ISI PRODUK:\n"))
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Lisensi disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Kode",
                            tint = SuccessGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SALIN LISENSI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SuccessGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==================== TAB 2: KELOLA PRODUK VIEW ====================

@Composable
fun ProductManagementView(
    products: List<Product>,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NVENTORÍ DATA PRODUK",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextLight,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "Kelola digital goods, setelan stock, dan lisensi auto-delivery",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }

        if (products.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = "No products",
                    tint = TextMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Belum Ada Produk Terdaftar",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextLight,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Silakan klik tombol + di bawah untuk menginput produk digital.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    ProductItemCard(
                        product = product,
                        onEdit = { onEditProduct(product) },
                        onDelete = { onDeleteProduct(product) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Tag Category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberSecondary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = product.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // Actions edit / delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Produk",
                            tint = CyberPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Produk",
                            tint = DangerRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Stock + Price + Sold
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rub = remember(product.price) {
                    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                    format.format(product.price).replace("Rp", "Rp ")
                }

                Text(
                    text = rub,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Stock Badge
                    val isOutOfStock = product.stock == 0
                    val stockBgColor = if (isOutOfStock) DangerRed.copy(alpha = 0.1f) else Color(0xFF1E293B)
                    val stockTextColor = if (isOutOfStock) DangerRed else TextLight

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(stockBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isOutOfStock) "Stok Habis" else "Stok: ${product.stock}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = stockTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Sold Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Terjual: ${product.soldCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            // Desc & Auto Delivery (visible when expanded)
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF1E293B))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Deskripsi Produk:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextLight.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Konten Auto-Delivery (Lisensi/Link):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateBackground, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product.autoDeliveryContent,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SuccessGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("secret_key", product.autoDeliveryContent)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Konten lisensi disalin!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin kunci produk",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== TAB 3: SETELAN AGEN VIEW ====================

@Composable
fun AgentSettingsView(
    viewModel: AgentBusinessViewModel,
    settings: AgentSettings
) {
    var name by remember(settings) { mutableStateOf(settings.agentName) }
    var tone by remember(settings) { mutableStateOf(settings.tone) }
    var rules by remember(settings) { mutableStateOf(settings.customRules) }
    var objective by remember(settings) { mutableStateOf(settings.businessObjective) }

    val tonesList = listOf("Friendly", "Professional", "Hustler", "Technical")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = "KONFÍGURASÍ INTELLIGENT AGENT",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextLight,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            Text(
                text = "Kustomisasi kepribadian, instruksi, dan taktik asisten AI Anda",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Agent Name Input
            item {
                Text(
                    text = "NAMA ASISTEN AI:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("agent_name_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateSurface,
                        unfocusedContainerColor = SlateSurface,
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    )
                )
            }

            // Persona Tone Selector
            item {
                Text(
                    text = "GAYA / NADA PERSONA AI:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tonesList.forEach { itemTone ->
                        val isSelected = tone == itemTone
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberPrimary else SlateSurface)
                                .clickable { tone = itemTone }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = itemTone,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) SlateBackground else TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Custom Auto-Delivery Guidelines/Rules
            item {
                Text(
                    text = "ATURAN KHUSUS BISNIS (PROMPT INSTRUCTION):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .testTag("agent_instructions_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateSurface,
                        unfocusedContainerColor = SlateSurface,
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    maxLines = 4
                )
            }

            // Target Objective
            item {
                Text(
                    text = "PROGRAM TARGET UTAMA (OBJECTIVE):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = objective,
                    onValueChange = { objective = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateSurface,
                        unfocusedContainerColor = SlateSurface,
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    maxLines = 2
                )
            }

            // Save settings action
            item {
                Button(
                    onClick = {
                        val updated = settings.copy(
                            agentName = name,
                            tone = tone,
                            customRules = rules,
                            businessObjective = objective
                        )
                        viewModel.updateSettings(updated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                ) {
                    Text(
                        text = "SIMPAN KONFIGURASI",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = SlateBackground,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // System API Key warning card (as mandated in Android Secret Management)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Security Alert",
                                tint = DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SECURITY WARNING - DECOMPILATION SAFETY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DangerRed,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==================== COMMON FORM DIALOG FOR PRODUCTS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    title: String,
    product: Product? = null,
    onSubmit: (String, String, Double, Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Voucher") }
    var priceStr by remember { mutableStateOf(product?.price?.toInt()?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var deliverySecret by remember { mutableStateOf(product?.autoDeliveryContent ?: "") }

    val categories = listOf("Voucher", "E-Book", "License Key", "Premium Account")
    var isMenuExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextLight,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // Name
                item {
                    Text("NAMA PRODUK DIGITAL:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_product_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        singleLine = true
                    )
                }

                // CategoryDropdown
                item {
                    Text("KATEGORI PRODUK:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    Spacer(modifier = Modifier.height(2.dp))
                    ExposedDropdownMenuBox(
                        expanded = isMenuExpanded,
                        onExpandedChange = { isMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberPrimary,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            categories.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(text = selectionOption) },
                                    onClick = {
                                        category = selectionOption
                                        isMenuExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                // Price and Stock
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("HARGA (RP):", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = priceStr,
                                onValueChange = { priceStr = it },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_product_price"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimary,
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(0.8f)) {
                            Text("STOK AWAL:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = stockStr,
                                onValueChange = { stockStr = it },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_product_stock"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberPrimary,
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }

                // Description
                item {
                    Text("DESKRIPSI PRODUK:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        maxLines = 3
                    )
                }

                // Auto Delivery Content (The voucher/key content sent after checkout)
                item {
                    Text(
                        text = "KONTEN KIRIM SECARA OTOMATIS (LISENSI/KUNCI/LINK):",
                        style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = deliverySecret,
                        onValueChange = { deliverySecret = it },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_product_delivery"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SuccessGreen,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        maxLines = 4,
                        placeholder = { Text("Isi kode lisensi atau url download yang dikirim ke pelanggan setelah dipotong stok.", fontSize = 11.sp, color = TextMuted) }
                    )
                }

                // Form Actions
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted)
                        ) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val price = priceStr.toDoubleOrNull() ?: 0.0
                                val stock = stockStr.toIntOrNull() ?: 0
                                if (name.isNotBlank() && price > 0.0 && stock >= 0 && deliverySecret.isNotBlank()) {
                                    onSubmit(name, category, price, stock, description, deliverySecret)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                            modifier = Modifier.testTag("dialog_save_button")
                        ) {
                            Text("Simpan", color = SlateBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Border Stroke Helper
@Composable
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PromoProspectView(
    viewModel: AgentBusinessViewModel,
    products: List<Product>,
    onGoToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- State collections from ViewModel ---
    val potentialClients by viewModel.targetPotentialClients.collectAsState()
    val isSearching by viewModel.isSearchingClients.collectAsState()
    val isGeneratingPromo by viewModel.isGeneratingPromoState.collectAsState()

    // --- Local Screen States ---
    var selectedProductForPromo by remember { mutableStateOf<Product?>(products.firstOrNull()) }
    var selectedChannel by remember { mutableStateOf("WhatsApp") } // WhatsApp, Email, Instagram/Medsos
    var customTargetNiche by remember { mutableStateOf("Mahasiswa & Pekerja Muda") }
    var generatedPromoCopy by remember { mutableStateOf("") }

    // If selected product is null or became invalid, pick first product available
    LaunchedEffect(products) {
        if (selectedProductForPromo == null && products.isNotEmpty()) {
            selectedProductForPromo = products.first()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- SECTION HEADER ---
        item {
            Column {
                Text(
                    text = "AI PROMO & CUSTOMER HUNTER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextLight,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "Gunakan asisten kecerdasan buatan untuk merancang naskah promosi persuasif dan memburu 5 calon prospek pembeli produk digital Anda.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }

        if (products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "No products",
                            tint = DangerRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Belum Ada Produk Terdaftar",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextLight, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Anda harus mendaftarkan setidaknya satu produk digital di tab 'Produk' terlebih dahulu agar AI dapat menyusun kampanye promo.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, textAlign = TextAlign.Center),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        } else {
            // --- BOX 1: AUTO PROMOTIONAL COPYWRITER ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Promo Generator",
                                tint = CyberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1. ASISTEN COPYWRITING PROMOSI (AI)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        // Select Product
                        Text(
                            text = "PILIH PRODUK DIGITAL UNTUK DIKAMPANYEKAN:",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            products.forEach { prod ->
                                val isSelected = selectedProductForPromo?.id == prod.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyberPrimary.copy(alpha = 0.15f) else SlateBackground)
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberPrimary else Color(0xFF1E293B),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedProductForPromo = prod }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isSelected) CyberPrimary else TextLight,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Channel selector
                        Text(
                            text = "PILIH SALURAN PROMOSI:",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("WhatsApp", "Email", "Instagram/Medsos").forEach { channel ->
                                val isSelected = selectedChannel == channel
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyberSecondary.copy(alpha = 0.15f) else SlateBackground)
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberSecondary else Color(0xFF1E293B),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedChannel = channel }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = channel,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isSelected) CyberSecondary else TextLight,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val prod = selectedProductForPromo
                                    if (prod != null) {
                                        generatedPromoCopy = viewModel.generatePromoMsg(prod.name, prod.description, selectedChannel)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                            enabled = !isGeneratingPromo
                        ) {
                            if (isGeneratingPromo) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SlateBackground, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sedang Menyusun Naskah...", color = SlateBackground, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Susun Naskah Copywriting Kreatif", color = SlateBackground, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Display Generated Copy
                        if (generatedPromoCopy.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlateBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "COPYWRITING SIAP SIAR ($selectedChannel):",
                                            style = MaterialTheme.typography.labelSmall.copy(color = CyberSecondary, fontWeight = FontWeight.Bold)
                                        )
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("promo_copy", generatedPromoCopy)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Naskah promo disalin!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Salin naskah",
                                                tint = CyberSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = generatedPromoCopy,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextLight,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- BOX 2: TARGET LEADS / BUYERS FINDER (PROSPEK) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Prospect Finder",
                                tint = CyberSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. DETEKTIF PEMBURU KLIEN POTENSIAL (AI)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Target Niche
                        Text(
                            text = "DESKRIPSI TARGET PASAR / NICHE TARGET:",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        OutlinedTextField(
                            value = customTargetNiche,
                            onValueChange = { customTargetNiche = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Misal: Mahasiswa IT, Pemilik Cafe, Gamers, Kreator Medsos", fontSize = 11.sp, color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberSecondary,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val prod = selectedProductForPromo
                                if (prod != null) {
                                    viewModel.searchClientsForProduct(prod.name, customTargetNiche)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                            enabled = !isSearching
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SlateBackground, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mengeksplor Database Klien...", color = SlateBackground, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Pindai Calon Pembeli Niche Ini", color = SlateBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- RENDER FOUND LEADS LIST ---
            if (potentialClients.isNotEmpty()) {
                item {
                    Text(
                        text = "5 PROSPEK TERPANTAU YANG COCOK BERDASARKAN AI:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                items(potentialClients) { client ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Client profile card header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👤 ${client.name}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = CyberSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberSecondary.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = client.profession.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Pain point and background
                            Text(
                                text = "Latar Belakang & Masalah Klien:",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = client.background,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextLight, fontSize = 12.sp, lineHeight = 15.sp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Pitch Strategy
                            Text(
                                text = "Playbook Pendekatan Promosi AI:",
                                style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = client.pitchStrategy,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextLight.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 15.sp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            // Action button: Initiate pitch campaign
                            Button(
                                onClick = {
                                    val prod = selectedProductForPromo
                                    if (prod != null) {
                                        // Auto-write customized draft pitch for this specific person
                                        val customDraft = "Halo Kak ${client.name}! Kami mengamati sebagai ${client.profession} kakak mungkin sedang butuh solusi praktis untuk kendala: ${client.background}.\n\nKami merekomendasikan penawaran khusus teruji: *${prod.name}*!\n\n${client.pitchStrategy}\n\nKetik 'Beli ${prod.name}' sekarang jika kakak berminat ya, mumpung stock masih ada!"
                                        
                                        viewModel.pitchAndConvertClient(client, prod, customDraft)
                                        Toast.makeText(context, "Meluncurkan pitch promosi! Mengalihkan ke dasbor...", Toast.LENGTH_LONG).show()
                                        onGoToDashboard()
                                    } else {
                                        Toast.makeText(context, "Silakan pilih produk digital terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = "Pitch",
                                        tint = SlateBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Kirim Promo & Simulasikan Jual Deal!",
                                        color = SlateBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

