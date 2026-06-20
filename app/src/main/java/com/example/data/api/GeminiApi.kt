package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AgentSettings
import com.example.data.model.Product
import com.example.data.model.PotentialClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    val apiKey: String = BuildConfig.GEMINI_API_KEY

    // OkHttpClient with 60s timeouts as strictly mandated in the Gotchas section
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Helper data structures for JSON matching
    private class GeminiRequest(
        val contents: List<ContentJson>,
        val systemInstruction: ContentJson? = null,
        val generationConfig: GenerationConfigJson? = null
    )

    private class ContentJson(val parts: List<PartJson>)
    private class PartJson(val text: String)
    private class GenerationConfigJson(val temperature: Float = 0.7f)

    /**
     * Checks if a valid, non-placeholder API key is available
     */
    fun isKeyConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && !apiKey.contains("PLACEHOLDER")
    }

    /**
     * Generates a response from Gemini based on customer history, current message, product inventory & settings
     */
    suspend fun getAgentResponse(
        customerMessage: String,
        conversationHistory: List<Pair<String, String>>, // list of Pair(senderName, messageText)
        products: List<Product>,
        settings: AgentSettings
    ): Pair<String, String> = withContext(Dispatchers.IO) { // Return Pair(ActualTextResponse, ThoughtLog)
        
        val productInventoryText = products.joinToString("\n") { p ->
            "- ID: ${p.id} | Nama: ${p.name} | Kategori: ${p.category} | Harga: Rp ${p.price} | Stok: ${p.stock} | Deskripsi: ${p.description}"
        }

        val systemPrompt = """
            Anda adalah Chatbot Agen Asisten Bisnis kecerdasan buatan bernama ${settings.agentName}.
            Tugas Anda adalah melayani pelanggan secara cerdas dan mandiri untuk bisnis produk digital otomatis pemilik Anda.
            
            [PANDUAN UTAMA]
            1. Nada bicara Anda harus: ${settings.tone}.
            2. Aturan bisnis khusus yang harus dipatuhi: ${settings.customRules}.
            3. Objektif bisnis Anda: ${settings.businessObjective}.
            
            [INFORMASI INVENTORY PRODAK DIGITAL SAAT INI]
            $productInventoryText
            
            [ATURAN TRANSAKSI otomatis (SANGAT PENTING!)]
            Jika pelanggan secara tegas mengonfirmasi ketertarikan kuat mereka untuk MEMBELI produk digital tertentu (contoh: "Oke saya mau beli e-book dropship", "Saya pesan Windows 11", "Beli spotify 1 bulan dong", "Mau beli canva pro") DAN produk tersebut memiliki STOK > 0:
            - Jawab pesanan pelanggan dengan ramah dan profesional.
            - DI AKHIR RESPONSE Anda, Anda WAJIB menambahkan format persis berikut ini tanpa modifikasi spasi: `[ACTION:BUY_PRODUCT_ID:X]` di mana X adalah ID numerik produk digital tersebut.
            - Contoh: "Baik kak, pesanan Windows 11 Pro segera kami proses ya. Mohon tunggu sebentar... [ACTION:BUY_PRODUCT_ID:2]"
            - Jika produk tersebut STOK-nya 0, sampaikan rincian maaf bahwa barang sedang kosong, jangan tambahkan kode aksi tersebut.
            
            [KONTEKS LAINNYA]
            - Jawab pertanyaan seputar fitur produk dengan detail dan profesional berdasarkan rincian deskripsi di inventori.
            - Jangan buat janji produk yang tidak terdaftar di daftar di atas.
            - Tulis rincian jawaban Anda dalam Bahasa Indonesia yang alami sesuai nada yang diinginkan.
        """.trimIndent()

        if (!isKeyConfigured()) {
            // GRACEFUL CHECK: Fallback simulator mode
            Log.w(TAG, "API Key is missing or using placeholder! Running smart simulated fallback engine.")
            return@withContext runSimulatedAgent(customerMessage, conversationHistory, products, settings)
        }

        // Build history in Gemini request format
        val chatParts = mutableListOf<PartJson>()
        for (turn in conversationHistory) {
            val prefix = if (turn.first == "CUSTOMER") "Customer: " else if (turn.first == "SYSTEM") "System Automation: " else "Saya (${settings.agentName}): "
            chatParts.add(PartJson("${prefix}${turn.second}"))
        }
        chatParts.add(PartJson("Customer: $customerMessage"))

        val reqObj = GeminiRequest(
            contents = listOf(ContentJson(parts = chatParts)),
            systemInstruction = ContentJson(parts = listOf(PartJson(systemPrompt))),
            generationConfig = GenerationConfigJson(temperature = 0.6f)
        )

        val jsonAdapter = moshi.adapter(GeminiRequest::class.java)
        val reqJson = jsonAdapter.toJson(reqObj)

        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(reqJson.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API Request failed: Code ${response.code}, Body: $errorBody")
                    return@withContext Pair(
                        "Maaf kak, koneksi asisten AI sedang sibuk. Silakan coba kirim ulang pesan Anda. (API Error ${response.code})",
                        "Gagal menghubungi server Gemini: ${response.code} - $errorBody"
                    )
                }

                val responseBodyStr = response.body?.string() ?: ""
                val adapter = moshi.adapter(Map::class.java)
                val responseMap = adapter.fromJson(responseBodyStr)
                
                val candidatesList = responseMap?.get("candidates") as? List<*>
                val firstCandidate = candidatesList?.getOrNull(0) as? Map<*, *>
                val contentMap = firstCandidate?.get("content") as? Map<*, *>
                val partsList = contentMap?.get("parts") as? List<*>
                val firstPart = partsList?.getOrNull(0) as? Map<*, *>
                val responseText = firstPart?.get("text") as? String ?: ""

                if (responseText.isBlank()) {
                    return@withContext Pair(
                    "Halo! Ada yang bisa kami bantu seputar produk digital hari ini?",
                    "Menerima respons kosong dari server Gemini."
                    )
                }

                // AI succeeded, calculate simulated thinking
                val thinkingText = "Menganalisa input pelanggan.\n" +
                        "Persona: ${settings.agentName} (${settings.tone})\n" +
                        "Aturan Bisnis: Diaplikasikan.\n" +
                        "Logika: Respons berhasil dibuat menggunakan model gemini-3.5-flash."

                Pair(responseText, thinkingText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing generation", e)
            Pair(
                "Ups, sepertinya asisten mengalami sedikit kendala jaringan. Tulis pesan Anda lagi ya! (Exception: ${e.localizedMessage})",
                "Koneksi Exception: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Smart fallback local agent for seamless offline/unconfigured testing
     */
    private fun runSimulatedAgent(
        message: String,
        history: List<Pair<String, String>>,
        products: List<Product>,
        settings: AgentSettings
    ): Pair<String, String> {
        val msgLower = message.lowercase()
        val responseText: String
        val buyAction: String

        // Find match in products
        val matchedProduct = products.find { p ->
            msgLower.contains(p.name.lowercase().substringBefore(":").substringBefore(" ")) ||
            p.category.lowercase().split(" ").any { cat -> msgLower.contains(cat) } ||
            msgLower.contains("beli") && msgLower.contains(p.category.lowercase()) ||
            msgLower.contains(p.name.lowercase().split(" ").first().lowercase())
        }

        val greetingText = when (settings.tone) {
            "Professional" -> "Selamat datang di Layanan Otomatis ${settings.agentName}. Saya siap membantu mengelola rincian produk digital Anda."
            "Hustler" -> "Yo bos! Ada peluang profit apa hari ini? ${settings.agentName} siap bantu deal instan!"
            "Technical" -> "Sistem operasional ${settings.agentName} aktif. Query terdeteksi. Bagaimana saya bisa mengoptimasi pemecahan isu Anda?"
            else -> "Halo Kak! Salam kenal, aku ${settings.agentName}. Ada paket produk digital keren yang bisa kubantu jelaskan hari ini? 😊"
        }

        if (msgLower.contains("halo") || msgLower.contains("hei") || msgLower.contains("hallo") || msgLower.contains("assalamualaikum") || msgLower.contains("hi")) {
            responseText = "$greetingText\n\nKami melayani pembelian produk digital premium otomatis seperti E-Book, Lisensi OS, maupun Akun Premium."
            buyAction = ""
        } else if (msgLower.contains("harga") || msgLower.contains("daftar") || msgLower.contains("inventory") || msgLower.contains("stok") || msgLower.contains("list") || msgLower.contains("produk")) {
            val listText = products.joinToString("\n") { "- *${it.name}* (${it.category}) = Rom Rp ${it.price} [Stok: ${it.stock}]" }
            responseText = "Berikut adalah daftar inventori aktif kami saat ini, Kak:\n\n$listText\n\nTulis nama produk atau ketik 'Beli <nama produk>' untuk memesan langsung lewat sistem otomatis kami!"
            buyAction = ""
        } else if (msgLower.contains("beli") || msgLower.contains("order") || msgLower.contains("mau") || msgLower.contains("pesan") || msgLower.contains("checkout")) {
            if (matchedProduct != null) {
                if (matchedProduct.stock > 0) {
                    val customGreet = when (settings.tone) {
                        "Professional" -> "Terima kasih atas keputusan Anda. Kami akan segera me-log pesanan Anda pada database kami."
                        "Hustler" -> "Mantap bos! Pilihan produk paling menguntungkan! Pembayaran langsung diautentikasi otomatis."
                        "Technical" -> "Menginisiasi transaksi API produk ID ${matchedProduct.id}. Menjalankan parameter pengiriman instan."
                        else -> "Wah pilihan bagus kak! Aku bantu proses checkout pesanan *${matchedProduct.name}* sekarang ya."
                    }
                    responseText = "$customGreet Mohon tunggu sementara sistem memproses pengiriman digital Anda...\n\n[ACTION:BUY_PRODUCT_ID:${matchedProduct.id}]"
                    buyAction = "Terdeteksi niat beli produk ID ${matchedProduct.id}."
                } else {
                    responseText = "Aduh maaf sekali kak, item *${matchedProduct.name}* saat ini sedang habis terjual. Kami akan segera memberi tahu vendor untuk restock!"
                    buyAction = ""
                }
            } else {
                responseText = "Metode pesanan terdeteksi. Untuk membeli, mohon cantumkan nama produk digital yang jelas dari daftar kami (Contoh: 'beli canva pro' atau 'beli ebook dropship')."
                buyAction = ""
            }
        } else if (matchedProduct != null && (msgLower.contains("apa") || msgLower.contains("jelas") || msgLower.contains("gimana") || msgLower.contains("bagaimana") || msgLower.contains("detail") || msgLower.contains("fitur") || msgLower.contains("tanya"))) {
            responseText = "Tentu kak! Berikut info lengkap mengenai *${matchedProduct.name}*:\n\n*Kategori:* ${matchedProduct.category}\n*Harga:* Rp ${matchedProduct.price}\n*Stok:* ${matchedProduct.stock} unit\n\n*Deskripsi:* ${matchedProduct.description}\n\nKetik 'Beli ${matchedProduct.name}' jika sudah mantap ya!"
            buyAction = ""
        } else {
            responseText = "Saya memahami pertanyaan Anda mengenai digital product business. Ada hal khusus lain seputar fungsionalitas, restock item, atau lisensi yang ingin ditanyakan kepada ${settings.agentName}?\n\nKetik 'list produk' untuk melihat katalog kami."
            buyAction = ""
        }

        val thoughts = "Simulasi Offline Engine (${settings.tone}):\n" +
                "- Input: '$message'\n" +
                "- Status Kunci: Belum dikonfigurasi (Simulation Mode aktif)\n" +
                "- Evaluasi Kategori: " + (matchedProduct?.category ?: "Umum") + "\n" +
                "- Keputusan Tindakan: " + (if (buyAction.isNotEmpty()) buyAction else "Kirim respons teks biasa")

        return Pair(responseText, thoughts)
    }

    /**
     * Generates a conversational/persuasive promotion script
     */
    suspend fun generatePromoPlan(
        productName: String,
        productDesc: String,
        channel: String
    ): String = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext getSimulatedPromoPlan(productName, productDesc, channel)
        }

        val systemPrompt = """
            Anda adalah seorang praktisi marketing digital pakar copywriting konversi tinggi di Indonesia.
            Tugas Anda adalah menulis naskah/copy promosi persuasif berbahasa Indonesia untuk produk digital '$productName' yang memiliki rincian deskripsi: '$productDesc'.
            Buat naskah promosi eksklusif yang sangat disesuaikan dengan saluran promosi: '$channel'.
            Format promo harus siap pakai, rapi, menarik, menggunakan emoji yang relevan, memiliki hook pencuri perhatian di awal, rincian benefit utama, call-to-action yang kuat, dan batas waktu kelangkaan (scarcity).
        """.trimIndent()

        val chatParts = listOf(PartJson("Buatkan naskah penawaran untuk produk '$productName' di saluran '$channel' sekarang."))

        val reqObj = GeminiRequest(
            contents = listOf(ContentJson(parts = chatParts)),
            systemInstruction = ContentJson(parts = listOf(PartJson(systemPrompt))),
            generationConfig = GenerationConfigJson(temperature = 0.8f)
        )

        val jsonAdapter = moshi.adapter(GeminiRequest::class.java)
        val reqJson = jsonAdapter.toJson(reqObj) ?: ""

        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(reqJson.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext getSimulatedPromoPlan(productName, productDesc, channel)
                }

                val responseBodyStr = response.body?.string() ?: ""
                val adapter = moshi.adapter(Map::class.java)
                val responseMap = adapter.fromJson(responseBodyStr)
                val candidatesList = responseMap?.get("candidates") as? List<*>
                val firstCandidate = candidatesList?.getOrNull(0) as? Map<*, *>
                val contentMap = firstCandidate?.get("content") as? Map<*, *>
                val partsList = contentMap?.get("parts") as? List<*>
                val firstPart = partsList?.getOrNull(0) as? Map<*, *>
                val responseText = firstPart?.get("text") as? String ?: ""
                
                if (responseText.isNotBlank()) responseText else getSimulatedPromoPlan(productName, productDesc, channel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating promo script", e)
            getSimulatedPromoPlan(productName, productDesc, channel)
        }
    }

    /**
     * Generates a list of targeted prospect clients for a product
     */
    suspend fun generatePotentialClients(
        productName: String,
        targetNiche: String
    ): List<PotentialClient> = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext getSimulatedPotentialClients(productName, targetNiche)
        }

        val systemPrompt = """
            Anda adalah Agen Riset Pasar Bisnis berpengalaman.
            Tugas Anda adalah memetakan dan merancang 5 profil Calon Klien/Pembeli Potensial di Indonesia untuk produk digital '$productName' dengan rentang niche target: '$targetNiche'.
            
            Anda WAJIB memberikan jawaban berupa teks terstruktur rapi dengan format baris baru di awali "##" sebagai pemisah antar prospek, dan pembatas kolom menggunakan karakter "||".
            Setiap prospek harus mengikuti pola kolom persis:
            NAMA||PROFESI/NICHE||KETERTARIKAN UTAMA||MASALAH/LATAR BELAKANG||PLAYBOOK PENDEKATAN PROMO
            
            Contoh Output:
            ##Riana Siregar||Pemilik Kedai Kopi||Canva Pro Subscription||Butuh desain menu, banner, dan reels instagram mingguan tapi hemat biaya desainer||Tawarkan canva premium bundle garansi ganti baru + template menu kopi siap saji.
            ##Farid Gunawan||Mahasiswa Akhir IF||Windows 11 License Key||Butuh OS original stabil bebas virus/malware untuk skripsi kodingan||Gencarkan garansi luntur, aktivasi legal Microsoft tanpa crack berbahaya.
            
            Pastikan Anda menghasilkan tepat 5 prospek. Jangan tampilkan teks pengantar, penutup, markdown bold, atau tanda kutip di luar format separator. Mulai respons langsung dengan ##.
        """.trimIndent()

        val chatParts = listOf(PartJson("Hasilkan 5 calon pembeli potensial untuk '$productName' niche '$targetNiche' sesuai format separator."))

        val reqObj = GeminiRequest(
            contents = listOf(ContentJson(parts = chatParts)),
            systemInstruction = ContentJson(parts = listOf(PartJson(systemPrompt))),
            generationConfig = GenerationConfigJson(temperature = 0.7f)
        )

        val jsonAdapter = moshi.adapter(GeminiRequest::class.java)
        val reqJson = jsonAdapter.toJson(reqObj) ?: ""

        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(reqJson.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext getSimulatedPotentialClients(productName, targetNiche)
                }

                val responseBodyStr = response.body?.string() ?: ""
                val adapter = moshi.adapter(Map::class.java)
                val responseMap = adapter.fromJson(responseBodyStr)
                val candidatesList = responseMap?.get("candidates") as? List<*>
                val firstCandidate = candidatesList?.getOrNull(0) as? Map<*, *>
                val contentMap = firstCandidate?.get("content") as? Map<*, *>
                val partsList = contentMap?.get("parts") as? List<*>
                val firstPart = partsList?.getOrNull(0) as? Map<*, *>
                val responseText = firstPart?.get("text") as? String ?: ""

                if (responseText.isBlank()) {
                    return@withContext getSimulatedPotentialClients(productName, targetNiche)
                }

                val clients = mutableListOf<PotentialClient>()
                val rawLines = responseText.split("##")
                for (line in rawLines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    val parts = trimmed.split("||")
                    if (parts.size >= 5) {
                        clients.add(
                            PotentialClient(
                                name = parts[0].trim(),
                                profession = parts[1].trim(),
                                interest = parts[2].trim(),
                                background = parts[3].trim(),
                                pitchStrategy = parts[4].trim()
                            )
                        )
                    }
                }

                if (clients.isNotEmpty()) clients else getSimulatedPotentialClients(productName, targetNiche)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating potential clients", e)
            getSimulatedPotentialClients(productName, targetNiche)
        }
    }

    private fun getSimulatedPromoPlan(name: String, desc: String, channel: String): String {
        val hook = when (channel) {
            "WhatsApp" -> "📢 *PENAWARAN SPESIAL TERBATAS!* 📢\n\nHalo Kak! Ada kabar gembira buat kamu yang ingin meningkatkan efisiensi dan produktivitas."
            "Email" -> "Subject: Jauhkan kendala operasional Anda sekarang juga dengan $name! 🚀\n\nYth. Rekan Bisnis,\n\nApakah Anda sering mengalami hambatan dalam mengelola pekerjaan digital harian Anda?"
            else -> "✨ *RESTOCK SPESIAL: $name IS HERE!* ✨\n\nCapek dengan batasan tools gratisan yang bikin pusing? Sini kumpul! 👇"
        }

        return """
            $hook
            
            Memperkenalkan solusi terbaik kami: *"$name"*!
            
            💡 *Kenapa Harus $name?*
            - $desc
            - Proses setup instan & didukung penuh autopilot bot 24 jam!
            - Jaminan lisensi legal & anti-blokir (Garansi uang kembali 100%).
            
            🎁 *Promo Khusus Hari Ini:*
            Gunakan kode kupon *SUPERPROMO* saat checkout untuk mendapatkan harga spesial langsung dari bot autopilot kami.
            
            👇 *Pesan Sekarang Hubungi Bot Kami:*
            Ketik saja *"Beli $name"* di kolom chat cockpit simulasi untuk transaksi otomatis super cepat!
            
            ⚡ _Jangan sampai kehabisan! Penawaran ini hanya berlaku hingga jam 23:59 WIB malam ini._
        """.trimIndent()
    }

    private fun getSimulatedPotentialClients(productName: String, targetNiche: String): List<PotentialClient> {
        val prodLower = productName.lowercase()
        val defaultList = when {
            prodLower.contains("canva") -> listOf(
                PotentialClient(name = "Geby Amalia", profession = "Content Creator Pemula", interest = "Canva Pro", background = "Sering edit feed Instagram & YouTube thumbnail tetapi budget per desainer mahal.", pitchStrategy = "Tawarkan canva premium murah legal dengan bonus 100 template feed."),
                PotentialClient(name = "Rangga Wijaya", profession = "UI/UX Freelancer", interest = "Aset Kolaborasi Canva", background = "Membutuhkan fitur kolaborasi real-time untuk koordinasi layout digital.", pitchStrategy = "Sebutkan kemudahan share link edit workspace tanpa download file manual."),
                PotentialClient(name = "Mega Kurnia", profession = "Pemilik Bisnis Hijab", interest = "Desain Instan", background = "Perlu membuat infografis diskon flash sale berkala secara instan tiap sore.", pitchStrategy = "Fokus pada efisiensi fitur drag-&-drop instan premium."),
                PotentialClient(name = "Bambang Santoso", profession = "Guru Honorer SMA", interest = "Canva Edu / Pro", background = "Ingin membuat slide presentasi kreatif yang menarik fokus siswa.", pitchStrategy = "Berikan opsi diskon instan dan panduan tutorial canva slides gratis."),
                PotentialClient(name = "Syera Monica", profession = "Agensi Medsos Founder", interest = "Canva Team Workspace", background = "Admin desainer barunya butuh akses ribuan font dan elemen premium berlisensi.", pitchStrategy = "Sediakan bundle lisensi khusus team multi-user hemat 40%.")
            )
            prodLower.contains("windows") || prodLower.contains("license") || prodLower.contains("key") -> listOf(
                PotentialClient(name = "Wahyu Hidayat", profession = "Gamer PC Builder", interest = "Windows 11 Pro Key", background = "Baru merakit PC gaming impian tapi OS terpasang watermark tidak berlisensi.", pitchStrategy = "Tekankan keaslian key Retail dan kemudahan aktivasi direct Microsoft."),
                PotentialClient(name = "Sinta Nur", profession = "Admin Kantor Notaris", interest = "Windows Retail License", background = "Komputer kantor rentan malware akibat sistem operasi palsu tanpa update.", pitchStrategy = "Soroti jaminan keamanan security update otomatis instan Microsoft."),
                PotentialClient(name = "Dewi Lestari", profession = "Owner Game Center / Warnet", interest = "Lisensi Windows Grosir", background = "Butuh melegalisasi 10 PC di unit usahanya agar tenang menghindari razia software.", pitchStrategy = "Tawarkan harga grosir bundling volume-based termurah."),
                PotentialClient(name = "Hendra Wijaya", profession = "Mahasiswa Teknik Elektro", interest = "Windows Upgrade 11 Pro", background = "Membutuhkan fitur Sandbox & Hyper-V bawaan Pro untuk praktikum perkuliahan.", pitchStrategy = "Jelaskan proses upgrade instan tanpa install ulang OS dari versi Home."),
                PotentialClient(name = "Agus Pratama", profession = "Sysadmin Lab Kampus", interest = "Windows Server Volume", background = "Perlu mensetting 30 komputer lab dengan OS berlisensi stabil anti-troubleshoot.", pitchStrategy = "Tawarkan support setup jarak jauh gratis via TeamViewer/AnyDesk.")
            )
            prodLower.contains("chatgpt") || prodLower.contains("ai") || prodLower.contains("premium") -> listOf(
                PotentialClient(name = "Farhan Saputra", profession = "SEO Copywriter Agency", interest = "ChatGPT Plus Shared", background = "Mengejar deadline ratusan artikel blog mingguan bebas limitasi reguler.", pitchStrategy = "Promosikan ketersediaan GPT-4o kecepatan tinggi anti-lag."),
                PotentialClient(name = "Linda Sari", profession = "Mahasiswa Kedokteran", interest = "Summarize tool asisten", background = "Sering kesulitan membaca tumpukan PDF jurnal sains berbahasa Inggris.", pitchStrategy = "Tunjukkan demo perintah pintas AI meringkas dokumen dalam 5 detik."),
                PotentialClient(name = "Deni Kusuma", profession = "Web Developer Indie", interest = "Kodingan Assistant AI", background = "Sering menemukan error sintaks di projek React-nya dan minim teman berdiskusi.", pitchStrategy = "Tekankan keandalan AI bertindak sebagai rekan kodingan premium."),
                PotentialClient(name = "Nadia Safitri", profession = "Food Blogger", interest = "ChatGPT Plus Writer", background = "Ingin rutin up artikel ulasan makanan tapi sering kena writer's block.", pitchStrategy = "Bagikan resep prompt viral siap pakai penarik clicks."),
                PotentialClient(name = "Rian Permana", profession = "E-Commerce Analyst", interest = "Analis Data AI", background = "Butuh membaca tren spreadsheet ekspor penjualan bulanan secara cepat.", pitchStrategy = "Fokus pada kehebatan fitur data analysis mutakhir.")
            )
            else -> listOf(
                PotentialClient(name = "Aldo Setiawan", profession = "Pegawai Swasta", interest = "Sampingan Digital", background = "Sedang mencari peluang bisnis digital dropship yang terbukti menghasilkan uang.", pitchStrategy = "Tawarkan materi penunjang + bonus video panduan dari nol."),
                PotentialClient(name = "Rini Amalia", profession = "Siswa SMK Digital", interest = "E-Book Panduan", background = "Butuh referensi materi praktis di luar kurikulum sekolah untuk belajar jualan.", pitchStrategy = "Berikan harga khusus student diskon 15% garansi kepuasan."),
                PotentialClient(name = "Fitri Handayani", profession = "Ibu Rumah Tangga", interest = "Bisnis Online Mudah", background = "Ingin membantu finansial suami dari rumah tanpa modal stok barang fisik.", pitchStrategy = "Sebutkan kemudahan jualan digital produk dengan margin profit 100%."),
                PotentialClient(name = "Zackry Ahmad", profession = "IT support Lepas", interest = "Software Utilities", background = "Kerap melayani jasa pemulihan file harddisk corrupt milik customer personal.", pitchStrategy = "Promosikan software pemulihan data instan sekali bayar."),
                PotentialClient(name = "Yuli Estetik", profession = "Micro-Influencer", interest = "Digital Filters", background = "Ingin merapikan feed estetik miliknya demi kolaborasi endorse baru.", pitchStrategy = "Kirim contoh instan transformasi visual filter.")
            )
        }
        return defaultList
    }
}

