package com.sinii.nfcemulator

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class NFCManager(context: Context) {
    
    companion object {
        private const val TAG = "NFCManager"
        private const val CARDS_DIR = "nfc_cards"
        private const val CARDS_FILE = "saved_cards.json"
    }
    
    private var nfcAdapter: NfcAdapter? = null
    private val gson = Gson()
    private val cardsDir: File
    private val cardsFile: File
    
    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        cardsDir = File(context.filesDir, CARDS_DIR)
        cardsFile = File(cardsDir, CARDS_FILE)
        
        // Create cards directory if it doesn't exist
        if (!cardsDir.exists()) {
            cardsDir.mkdirs()
        }
        
        Log.d(TAG, "NFCManager initialized with file storage at: ${cardsFile.absolutePath}")
    }
    
    fun isNfcAvailable(): Boolean {
        return nfcAdapter != null
    }
    
    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
    
    fun getNfcAdapter(): NfcAdapter? = nfcAdapter
    
    suspend fun readNFCTag(tag: Tag): NFCData? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Reading NFC tag: ${tag.id.contentToString()}")
            
            val id = NFCData.byteArrayToHex(tag.id)
            val techList = tag.techList.toList()
            val data = mutableMapOf<String, ByteArray>()
            
            Log.d(TAG, "Tag ID: $id")
            Log.d(TAG, "Available technologies: ${techList.joinToString()}")
            
            // Read data from different technologies
            techList.forEach { tech ->
                try {
                    when (tech) {
                        MifareClassic::class.java.name -> {
                            val mifare = MifareClassic.get(tag)
                            mifare.connect()
                            if (mifare.isConnected) {
                                val sectorData = mutableMapOf<String, ByteArray>()
                                for (sectorIndex in 0 until mifare.sectorCount) {
                                    val blockIndex = mifare.sectorToBlock(sectorIndex)
                                    try {
                                        val blockData = mifare.readBlock(blockIndex)
                                        sectorData["sector_$sectorIndex"] = blockData
                                        Log.d(TAG, "Sector $sectorIndex: ${NFCData.byteArrayToHex(blockData)}")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error reading sector $sectorIndex: ${e.message}")
                                    }
                                }
                                val allData = sectorData.values.flatMap { it.toList() }.toByteArray()
                                data[tech] = allData
                                mifare.close()
                            }
                        }
                        
                        MifareUltralight::class.java.name -> {
                            val mifare = MifareUltralight.get(tag)
                            mifare.connect()
                            if (mifare.isConnected) {
                                val pageData = mutableListOf<Byte>()
                                for (pageIndex in 0 until 16) {
                                    try {
                                        val page = mifare.readPages(pageIndex)
                                        pageData.addAll(page.toList())
                                        Log.d(TAG, "Page $pageIndex: ${NFCData.byteArrayToHex(page)}")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error reading page $pageIndex: ${e.message}")
                                    }
                                }
                                data[tech] = pageData.toByteArray()
                                mifare.close()
                            }
                        }
                        
                        Ndef::class.java.name -> {
                            val ndef = Ndef.get(tag)
                            ndef.connect()
                            if (ndef.isConnected) {
                                val ndefMessage = ndef.ndefMessage
                                if (ndefMessage != null) {
                                    val messageBytes = ndefMessage.toByteArray()
                                    data[tech] = messageBytes
                                    Log.d(TAG, "NDEF message: ${NFCData.byteArrayToHex(messageBytes)}")
                                } else {
                                    data[tech] = tag.id
                                    Log.d(TAG, "No NDEF message found, storing tag ID")
                                }
                                ndef.close()
                            }
                        }
                        
                        IsoDep::class.java.name -> {
                            val isoDep = IsoDep.get(tag)
                            isoDep.connect()
                            if (isoDep.isConnected) {
                                try {
                                    // Send SELECT command to get card info
                                    val selectCommand = byteArrayOf(0x00.toByte(),0xA4.toByte(), 0x04.toByte(), 0x00.toByte())
                                    val response = isoDep.transceive(selectCommand)
                                    data[tech] = response
                                    Log.d(TAG, "ISO-DEP response: ${NFCData.byteArrayToHex(response)}")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error with ISO-DEP: ${e.message}")
                                    data[tech] = tag.id
                                }
                                isoDep.close()
                            }
                        }
                        
                        else -> {
                            // For other technologies, just store the tag ID
                            data[tech] = tag.id
                            Log.d(TAG, "Technology $tech: ${NFCData.byteArrayToHex(tag.id)}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading technology $tech: ${e.message}")
                }
            }
            
            val nfcData = NFCData(id, techList, data)
            Log.d(TAG, "Successfully read NFC tag with ${data.size} technologies")
            return@withContext nfcData
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading NFC tag: ${e.message}")
            return@withContext null
        }
    }
    
    suspend fun saveCard(nfcData: NFCData, name: String? = null): NFCCard? {
        return try {
            // Check if card already exists to prevent duplicates
            if (isCardExists(nfcData.id)) {
                Log.d(TAG, "Card already exists: ${nfcData.id}")
                return getCardByTagId(nfcData.id)
            }
            
            val cardName = name ?: "NFC Card ${nfcData.id}"
            val card = NFCCard(
                id = nfcData.id,
                name = cardName,
                data = nfcData
            )
            
            // Save to file
            val currentCards = getSavedCardsList()
            Log.d(TAG, "Current cards before saving: ${currentCards.size}")
            val updatedCards = currentCards + card
            saveCardsToFile(updatedCards)
            Log.d(TAG, "Card saved to file: ${card.name} (${card.id}), total cards: ${updatedCards.size}")
            card
        } catch (e: Exception) {
            Log.e(TAG, "Error saving card to file: ${e.message}")
            null
        }
    }
    
    fun getSavedCards(): Flow<List<NFCCard>> = flow {
        val cards = getSavedCardsList()
        Log.d(TAG, "getSavedCards() called, emitting ${cards.size} cards")
        emit(cards)
    }
    
    suspend fun removeCard(cardId: String): Boolean {
        return try {
            val currentCards = getSavedCardsList()
            val updatedCards = currentCards.filter { it.id != cardId }
            
            if (updatedCards.size < currentCards.size) {
                saveCardsToFile(updatedCards)
                Log.d(TAG, "Card removed from file: $cardId")
                true
            } else {
                Log.w(TAG, "Card not found for removal: $cardId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing card from file: ${e.message}")
            false
        }
    }
    
    suspend fun getCardDataForEmulation(cardId: String): NFCData? {
        return try {
            val card = getSavedCardsList().find { it.id == cardId }
            card?.data
        } catch (e: Exception) {
            Log.e(TAG, "Error getting card data for emulation: ${e.message}")
            null
        }
    }
    
    suspend fun isCardExists(tagId: String): Boolean {
        return try {
            getSavedCardsList().any { it.id == tagId }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if card exists: ${e.message}")
            false
        }
    }
    
    private suspend fun getCardByTagId(tagId: String): NFCCard? {
        return try {
            getSavedCardsList().find { it.id == tagId }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting card by tag ID: ${e.message}")
            null
        }
    }
    
    private suspend fun getSavedCardsList(): List<NFCCard> = withContext(Dispatchers.IO) {
        try {
            if (!cardsFile.exists()) {
                Log.d(TAG, "Cards file does not exist: ${cardsFile.absolutePath}")
                return@withContext emptyList()
            }
            
            Log.d(TAG, "Cards file exists, size: ${cardsFile.length()} bytes")
            
            FileReader(cardsFile).use { reader ->
                val type = object : TypeToken<List<NFCCard>>() {}.type
                val cards: List<NFCCard> = gson.fromJson(reader, type) ?: emptyList()
                Log.d(TAG, "Loaded ${cards.size} saved cards from file")
                if (cards.isNotEmpty()) {
                    cards.forEach { card ->
                        Log.d(TAG, "Card: ${card.name} (${card.id})")
                    }
                }
                cards
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved cards from file: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    private suspend fun saveCardsToFile(cards: List<NFCCard>) = withContext(Dispatchers.IO) {
        try {
            FileWriter(cardsFile).use { writer ->
                gson.toJson(cards, writer)
            }
            Log.d(TAG, "Saved ${cards.size} cards to file: ${cardsFile.absolutePath}")
            
            // Verify the file was written correctly
            if (cardsFile.exists()) {
                Log.d(TAG, "File verification: exists=${cardsFile.exists()}, size=${cardsFile.length()} bytes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cards to file: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // Debug function to manually check file contents
    fun debugFileContents() {
        try {
            Log.d(TAG, "=== DEBUG FILE CONTENTS ===")
            Log.d(TAG, "File path: ${cardsFile.absolutePath}")
            Log.d(TAG, "File exists: ${cardsFile.exists()}")
            if (cardsFile.exists()) {
                Log.d(TAG, "File size: ${cardsFile.length()} bytes")
                val content = cardsFile.readText()
                Log.d(TAG, "File content: $content")
            }
            Log.d(TAG, "=== END DEBUG ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error debugging file contents: ${e.message}")
            e.printStackTrace()
        }
    }
}
