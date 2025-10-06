package com.sinii.nfcemulator

import android.nfc.cardemulation.HostApduService
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

class NFCEmulationService : HostApduService() {
    
    init {
        // This will run when an instance is created
        Log.d("NFCEmulationService", "=== SERVICE INSTANCE CREATED ===")
        Log.d("NFCEmulationService", "Instance created at: ${System.currentTimeMillis()}")
        Log.d("NFCEmulationService", "Instance hash: ${this.hashCode()}")
        
        // Force a log to see if this runs
        try {
            Log.d("NFCEmulationService", "=== INIT BLOCK EXECUTED ===")
        } catch (e: Exception) {
            Log.e("NFCEmulationService", "Init block error: ${e.message}")
        }
    }
    
    companion object {
        private const val TAG = "NFCEmulationService"
        private const val SELECT_APDU_HEADER = "00A40400"

        init {
            // This will run when the class is first loaded
            Log.d(TAG, "=== NFCEmulationService CLASS LOADED ===")
            Log.d(TAG, "Class loaded at: ${System.currentTimeMillis()}")
            Log.d(TAG, "Class loaded in process: ${android.os.Process.myPid()}")
            Log.d(TAG, "Class loaded in thread: ${android.os.Process.myTid()}")
        }
        
        // Global state to track which card is being emulated
        private val emulatedCardData = ConcurrentHashMap<String, NFCData>()
        private var currentEmulatedCardId: String? = null
        
        // LogManager instance for saving logs
        private var logManager: LogManager? = null
        
        fun setLogManager(manager: LogManager) {
            logManager = manager
        }
        
        /**
         * Save log to both Android Log and file
         */
        private fun saveLog(level: String, message: String) {
            // Always log to Android Log first
            when (level) {
                "D" -> Log.d(TAG, message)
                "I" -> Log.i(TAG, message)
                "W" -> Log.w(TAG, message)
                "E" -> Log.e(TAG, message)
                "V" -> Log.v(TAG, message)
            }
            
            // Also save to file if LogManager is available
            try {
                logManager?.saveLog(TAG, level, message)
            } catch (e: Exception) {
                Log.e(TAG, "LogManager saveLog failed: ${e.message}")
            }
        }
        
        // Add more AIDs for better compatibility
        private val SUPPORTED_AIDS = listOf(
            "F0010203040506",
            "F0010203040507", 
            "F0010203040508",
            "D2760000850101",
            "A0000002471001",
            "F0000000000000"
        )

        fun setEmulatedCard(cardId: String, data: NFCData?) {
            if (data != null) {
                emulatedCardData[cardId] = data
                currentEmulatedCardId = cardId
                
                // Force Android Log to see if this is called
                Log.d(TAG, "=== EMULATION STARTED (ANDROID LOG) ===")
                Log.d(TAG, "Card ID: $cardId")
                Log.d(TAG, "Card Technologies: ${data.techList}")
                Log.d(TAG, "Total emulated cards: ${emulatedCardData.size}")
                Log.d(TAG, "Current emulated card: $currentEmulatedCardId")
                
                saveLog("D", "=== EMULATION STARTED ===")
                saveLog("D", "Card ID: $cardId")
                saveLog("D", "Card Technologies: ${data.techList}")
                saveLog("D", "Card Data Size: ${data.data.values.sumOf { it.size }} bytes")
                saveLog("D", "Total emulated cards: ${emulatedCardData.size}")
                saveLog("D", "Current emulated card: $currentEmulatedCardId")
                saveLog("D", "=== END EMULATION START ===")
            } else {
                emulatedCardData.remove(cardId)
                if (currentEmulatedCardId == cardId) {
                    currentEmulatedCardId = null
                }
                saveLog("D", "=== EMULATION STOPPED ===")
                saveLog("D", "Removed emulated card: $cardId")
                saveLog("D", "Remaining emulated cards: ${emulatedCardData.size}")
                saveLog("D", "Current emulated card: $currentEmulatedCardId")
                saveLog("D", "=== END EMULATION STOP ===")
            }
        }
        
        fun getEmulatedCardData(): NFCData? {
            val data = emulatedCardData.values.firstOrNull()
            saveLog("D", "Getting emulated card data: ${data?.id ?: "null"}")
            return data
        }
        
        fun getCurrentEmulatedCardId(): String? {
            saveLog("D", "Current emulated card ID: $currentEmulatedCardId")
            return currentEmulatedCardId
        }
        
        fun getEmulationStatus(): String {
            val status = buildString {
                append("Emulation Status:\n")
                append("- Total emulated cards: ${emulatedCardData.size}\n")
                append("- Current emulated card: $currentEmulatedCardId\n")
                append("- Supported AIDs: $SUPPORTED_AIDS\n")
                append("- Service class: ${NFCEmulationService::class.java.name}\n")
                append("- Service package: com.sinii.nfcemulator\n")
                if (currentEmulatedCardId != null) {
                    val cardData = emulatedCardData[currentEmulatedCardId]
                    append("- Card data available: ${cardData != null}\n")
                    if (cardData != null) {
                        append("- Card ID: ${cardData.id}\n")
                        append("- Card technologies: ${cardData.techList}\n")
                        append("- Card data size: ${cardData.data.values.sumOf { it.size }} bytes\n")
                    }
                }
            }
            saveLog("D", status)
            return status
        }
    }
    
    /**
     * Create a READ BINARY response based on the card data
     */
    private fun createReadBinaryResponse(cardData: NFCData, commandApdu: ByteArray): ByteArray {
        saveLog("D", "Creating READ BINARY response for card: ${cardData.id}")
        
        // Extract offset from command (bytes 2-3)
        val offset = if (commandApdu.size >= 4) {
            ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
        } else 0
        
        saveLog("D", "READ BINARY offset: $offset")
        
        // Create a response with some card data
        val responseData = when {
            cardData.techList.contains("android.nfc.tech.MifareUltralight") -> {
                // Return page data for Mifare Ultralight
                val pageNumber = offset / 4
                if (pageNumber < cardData.data.size) {
                    cardData.data.values.elementAtOrNull(pageNumber) ?: ByteArray(4)
                } else {
                    ByteArray(4) { 0x00.toByte() }
                }
            }
            else -> {
                // Return card ID for other card types
                cardData.id.toByteArray()
            }
        }
        
        saveLog("D", "READ BINARY response data: ${NFCData.byteArrayToHex(responseData)}")
        return responseData + byteArrayOf(0x90.toByte(), 0x00.toByte())
    }
    
    /**
     * Create a Mifare READ response
     */
    private fun createMifareReadResponse(cardData: NFCData, commandApdu: ByteArray): ByteArray {
        saveLog("D", "Creating Mifare READ response for card: ${cardData.id}")
        
        // Extract page number from command (byte 1)
        val pageNumber = if (commandApdu.size >= 2) {
            commandApdu[1].toInt() and 0xFF
        } else 0
        
        saveLog("D", "Mifare READ page: $pageNumber")
        
        // Return page data if available, otherwise return zeros
        val responseData = if (pageNumber < cardData.data.size) {
            cardData.data.values.elementAtOrNull(pageNumber) ?: ByteArray(16)
        } else {
            ByteArray(16) { 0x00.toByte() }
        }
        
        saveLog("D", "Mifare READ response data: ${NFCData.byteArrayToHex(responseData)}")
        return responseData + byteArrayOf(0x90.toByte(), 0x00.toByte())
    }
    
    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        // IMMEDIATE Android Log - this should always work
        Log.d(TAG, "=== APDU COMMAND RECEIVED (IMMEDIATE) ===")
        Log.d(TAG, "Command length: ${commandApdu.size} bytes")
        Log.d(TAG, "Current emulated card: ${getCurrentEmulatedCardId()}")
        Log.d(TAG, "Total emulated cards: ${emulatedCardData.size}")
        Log.d(TAG, "Process ID: ${android.os.Process.myPid()}")
        Log.d(TAG, "Thread ID: ${android.os.Process.myTid()}")
        
        val commandHex = NFCData.byteArrayToHex(commandApdu)
        saveLog("D", "=== APDU COMMAND RECEIVED ===")
        saveLog("D", "Command (hex): $commandHex")
        saveLog("D", "Command length: ${commandApdu.size} bytes")
        saveLog("D", "Current emulated card: ${getCurrentEmulatedCardId()}")
        saveLog("D", "Total emulated cards: ${emulatedCardData.size}")
        saveLog("D", "Extras: $extras")
        
        // Log raw bytes for debugging
        saveLog("D", "Raw command bytes: ${commandApdu.joinToString(", ") { "0x%02X".format(it) }}")
        
        try {
            // FOR TESTING: Respond to ANY command with a simple response
            Log.d(TAG, "=== TESTING: RESPONDING TO ANY COMMAND ===")
            val testResponse = "TEST_CMD_${commandHex}".toByteArray() + byteArrayOf(0x90.toByte(), 0x00.toByte())
            Log.d(TAG, "Test response: ${NFCData.byteArrayToHex(testResponse)}")
            
            // Check if this is a SELECT command for our AID
            if (commandHex.startsWith(SELECT_APDU_HEADER)) {
                saveLog("D", "SELECT command received")
                saveLog("D", "Command hex: $commandHex")
                
                // Extract AID from SELECT command (bytes 5+)
                val aidLength = if (commandApdu.size >= 5) commandApdu[4].toInt() and 0xFF else 0
                val aidStart = 5
                val aidEnd = aidStart + aidLength
                
                saveLog("D", "AID length: $aidLength, start: $aidStart, end: $aidEnd")
                
                if (aidEnd <= commandApdu.size) {
                    val receivedAid = commandApdu.sliceArray(aidStart until aidEnd)
                    val receivedAidHex = NFCData.byteArrayToHex(receivedAid)
                    saveLog("D", "Received AID: $receivedAidHex")
                    saveLog("D", "Supported AIDs: $SUPPORTED_AIDS")
                    
                    // Check if the received AID is supported
                    if (SUPPORTED_AIDS.any { it.equals(receivedAidHex, ignoreCase = true) }) {
                        saveLog("D", "AID supported! Responding with SW_SUCCESS (9000)")
                        return byteArrayOf(0x90.toByte(), 0x00.toByte()) // SW_SUCCESS
                    } else {
                        saveLog("W", "AID not supported: $receivedAidHex")
                        return byteArrayOf(0x6A.toByte(), 0x82.toByte()) // SW_FILE_NOT_FOUND
                    }
                } else {
                    saveLog("W", "Invalid SELECT command format")
                    return byteArrayOf(0x6A.toByte(), 0x00.toByte()) // SW_WRONG_DATA
                }
            }
            
            // Get the emulated card data
            val cardData = getEmulatedCardData()
            if (cardData != null) {
                saveLog("D", "Processing command with emulated card: ${cardData.id}")
                saveLog("D", "Card technologies: ${cardData.techList}")
                
                // Handle different APDU commands based on the card type
                return when {
                    // READ BINARY command (common for many card types)
                    commandHex.startsWith("00B0") -> {
                        saveLog("D", "READ BINARY command detected")
                        val response = createReadBinaryResponse(cardData, commandApdu)
                        saveLog("D", "READ BINARY response: ${NFCData.byteArrayToHex(response)}")
                        response
                    }
                    
                    // GET UID command (Mifare cards)
                    commandHex.startsWith("FFCA") -> {
                        saveLog("D", "GET UID command detected")
                        val uid = cardData.id.toByteArray()
                        saveLog("D", "Returning UID: ${NFCData.byteArrayToHex(uid)}")
                        uid + byteArrayOf(0x90.toByte(), 0x00.toByte())
                    }
                    
                    // READ command (Mifare Ultralight)
                    commandHex.startsWith("30") -> {
                        saveLog("D", "READ command detected (Mifare Ultralight)")
                        val response = createMifareReadResponse(cardData, commandApdu)
                        saveLog("D", "READ response: ${NFCData.byteArrayToHex(response)}")
                        response
                    }
                    
                    // AUTHENTICATE command (Mifare Classic)
                    commandHex.startsWith("60") || commandHex.startsWith("61") -> {
                        saveLog("D", "AUTHENTICATE command detected (Mifare Classic)")
                        saveLog("D", "Responding with authentication success")
                        byteArrayOf(0x90.toByte(), 0x00.toByte())
                    }
                    
                    // UPDATE BINARY command
                    commandHex.startsWith("00D6") -> {
                        saveLog("D", "UPDATE BINARY command detected")
                        saveLog("D", "Acknowledging update")
                        byteArrayOf(0x90.toByte(), 0x00.toByte())
                    }
                    
                    // Test command for debugging
                    commandHex.startsWith("00") -> {
                        saveLog("D", "Test command detected: $commandHex")
                        val response = "TEST_RESP".toByteArray() + byteArrayOf(0x90.toByte(), 0x00.toByte())
                        saveLog("D", "Test response: ${NFCData.byteArrayToHex(response)}")
                        response
                    }
                    
                    else -> {
                        saveLog("D", "Unknown command: $commandHex")
                        saveLog("D", "Returning SW_SUCCESS for compatibility")
                        // For debugging, return a simple response to any unknown command
                        val response = "UNKNOWN_CMD".toByteArray() + byteArrayOf(0x90.toByte(), 0x00.toByte())
                        saveLog("D", "Unknown command response: ${NFCData.byteArrayToHex(response)}")
                        response
                    }
                }
            } else {
                saveLog("W", "No emulated card data available!")
                saveLog("W", "This should not happen if emulation is properly started")
                return byteArrayOf(0x6F.toByte(), 0x00.toByte()) // SW_UNKNOWN
            }
        } catch (e: Exception) {
            saveLog("E", "Error processing APDU command: ${e.message}")
            e.printStackTrace()
            return byteArrayOf(0x6F.toByte(), 0x00.toByte()) // SW_UNKNOWN
        } finally {
            saveLog("D", "=== END APDU PROCESSING ===")
        }
    }
    
    override fun onDeactivated(reason: Int) {
        saveLog("D", "=== SERVICE DEACTIVATED ===")
        saveLog("D", "Reason: $reason")
        when (reason) {
            DEACTIVATION_LINK_LOSS -> {
                saveLog("D", "Link lost - NFC reader moved away or connection broken")
            }
            DEACTIVATION_DESELECTED -> {
                saveLog("D", "Deselected - NFC reader stopped communicating")
            }
        }
        saveLog("D", "Current emulated card: ${getCurrentEmulatedCardId()}")
        saveLog("D", "=== END DEACTIVATION ===")
    }
    
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        saveLog("D", "=== SERVICE STARTED ===")
        saveLog("D", "Intent: $intent")
        saveLog("D", "Flags: $flags")
        saveLog("D", "Start ID: $startId")
        saveLog("D", "Current emulated cards: ${emulatedCardData.size}")
        saveLog("D", "Current emulated card: ${getCurrentEmulatedCardId()}")
        saveLog("D", "=== END SERVICE START ===")
        return super.onStartCommand(intent, flags, startId)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // IMMEDIATE Android Log - this should always work
        Log.d(TAG, "=== SERVICE CREATED (IMMEDIATE) ===")
        Log.d(TAG, "Service instance: ${this.hashCode()}")
        Log.d(TAG, "Service package: ${packageName}")
        Log.d(TAG, "Service class: ${javaClass.name}")
        
        saveLog("D", "=== SERVICE CREATED ===")
        saveLog("D", "Service instance: ${this.hashCode()}")
        saveLog("D", "Service package: ${packageName}")
        saveLog("D", "Service class: ${javaClass.name}")
        
        // Create notification channel for foreground service
        createNotificationChannel()
        
        // Force a log write to test if LogManager is working
        try {
            logManager?.saveLog(TAG, "D", "=== FORCED LOG TEST ===")
            saveLog("D", "LogManager is working!")
        } catch (e: Exception) {
            saveLog("E", "LogManager error: ${e.message}")
        }
        
        saveLog("D", "=== END SERVICE CREATION ===")
    }
    
    private fun createNotificationChannel() {
        try {
            val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.NotificationChannel(
                    "nfc_emulation",
                    "NFC Emulation",
                    android.app.NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows when NFC emulation is active"
                    setShowBadge(false)
                }
            } else {
                TODO("VERSION.SDK_INT < O")
            }

            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            saveLog("D", "Notification channel created successfully")
        } catch (e: Exception) {
            saveLog("E", "Error creating notification channel: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        saveLog("D", "=== SERVICE DESTROYED ===")
        saveLog("D", "Service instance: ${this.hashCode()}")
        saveLog("D", "=== END SERVICE DESTRUCTION ===")
    }
}
