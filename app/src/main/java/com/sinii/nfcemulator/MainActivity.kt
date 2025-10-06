package com.sinii.nfcemulator

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinii.nfcemulator.LogManager.Companion.TAG
import com.sinii.nfcemulator.ui.theme.NFCEmulatorTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var nfcManager: NFCManager
    private lateinit var logManager: LogManager
    private var nfcAdapter: NfcAdapter? = null
    private var currentCardUpdater: ((NFCData) -> Unit)? = null
    private var _currentCard = mutableStateOf<NFCData?>(null)
    
    private val nfcReaderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val nfcData = result.data?.getParcelableExtra<NFCData>(NFCReaderActivity.EXTRA_NFC_DATA)
            if (nfcData != null) {
                Log.d(TAG, "Received NFC data from reader: ${nfcData.id}")
                _currentCard.value = nfcData
                currentCardUpdater?.invoke(nfcData)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "=== onCreate called ===")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent data: ${intent.data}")
        Log.d(TAG, "Intent categories: ${intent.categories}")
        
        try {
            nfcManager = NFCManager(this)
            logManager = LogManager(this)
            nfcAdapter = nfcManager.getNfcAdapter()
            
            // Set LogManager in NFCEmulationService
            NFCEmulationService.setLogManager(logManager)
            
            Log.d(TAG, "NFC Manager initialized successfully")
            Log.d(TAG, "Log Manager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NFC Manager: ${e.message}")
        }
        
        setContent {
            NFCEmulatorTheme {
                NFCEmulatorApp(
                    nfcManager = nfcManager,
                    nfcAdapter = nfcAdapter,
                    logManager = logManager,
                    currentCard = _currentCard.value,
                    onNfcTagDiscovered = { tag -> handleNfcTag(tag) }
                )
            }
        }
        
        // Handle NFC intents that might have launched this activity
        handleNfcIntent(intent)
        
        Log.d(TAG, "=== onCreate completed ===")
    }

    
    private fun handleNfcIntent(intent: Intent) {
        Log.d(TAG, "=== handleNfcIntent called ===")
        Log.d(TAG, "Intent action: ${intent.action}")
        
        try {
            // Handle different types of NFC intents
            when (intent.action) {
                NfcAdapter.ACTION_TECH_DISCOVERED -> {
                    Log.d(TAG, "TECH_DISCOVERED intent received")
                    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                    if (tag != null) {
                        Log.d(TAG, "NFC tag found in TECH_DISCOVERED intent: ${tag.id.contentToString()}")
                        handleNfcTag(tag)
                    } else {
                        Log.d(TAG, "No tag found in TECH_DISCOVERED intent")
                    }
                }
                NfcAdapter.ACTION_TAG_DISCOVERED -> {
                    Log.d(TAG, "TAG_DISCOVERED intent received")
                    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                    if (tag != null) {
                        Log.d(TAG, "NFC tag found in TAG_DISCOVERED intent: ${tag.id.contentToString()}")
                        handleNfcTag(tag)
                    } else {
                        Log.d(TAG, "No tag found in TAG_DISCOVERED intent")
                    }
                }
                NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                    Log.d(TAG, "NDEF_DISCOVERED intent received")
                    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                    if (tag != null) {
                        Log.d(TAG, "NFC tag found in NDEF_DISCOVERED intent: ${tag.id.contentToString()}")
                        handleNfcTag(tag)
                    } else {
                        Log.d(TAG, "No tag found in NDEF_DISCOVERED intent")
                    }
                }
                else -> {
                    Log.d(TAG, "Non-NFC intent received: ${intent.action}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling NFC intent: ${e.message}")
            e.printStackTrace()
        }
        Log.d(TAG, "=== End handleNfcIntent ===")
    }
    
    private fun handleNfcTag(tag: Tag) {
        Log.d(TAG, "NFC tag discovered: ${tag.id.contentToString()}")
        
        // Launch the NFC reader activity
        val intent = Intent(this, NFCReaderActivity::class.java).apply {
            putExtra(NfcAdapter.EXTRA_TAG, tag)
        }
        nfcReaderLauncher.launch(intent)
    }
    
    override fun onResume() {
        super.onResume()
        try {
            nfcAdapter?.let { adapter ->
                if (adapter.isEnabled) {
                    Log.d(TAG, "NFC adapter is enabled, using manifest-based intent handling")
                } else {
                    Log.w(TAG, "NFC is not enabled")
                }
            } ?: run {
                Log.w(TAG, "NFC adapter is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking NFC status: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity paused")
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        Log.d(TAG, "=== onNewIntent called ===")
        Log.d(TAG, "Action: ${intent.action}")
        Log.d(TAG, "Data: ${intent.data}")
        Log.d(TAG, "Categories: ${intent.categories}")
        Log.d(TAG, "Flags: ${intent.flags}")
        
        // Handle NFC intents
        handleNfcIntent(intent)
        
        Log.d(TAG, "=== End onNewIntent ===")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCEmulatorApp(
    nfcManager: NFCManager,
    nfcAdapter: NfcAdapter?,
    logManager: LogManager?,
    currentCard: NFCData?,
    onNfcTagDiscovered: (Tag) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var savedCards by remember { mutableStateOf<List<NFCCard>>(emptyList()) }
    var isEmulating by remember { mutableStateOf(false) }
    var emulatedCardId by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var cardName by remember { mutableStateOf("") }
    
    // Collect saved cards from the database
    LaunchedEffect(Unit) {
        nfcManager.getSavedCards().collect { cards ->
            savedCards = cards
            Log.d("MainActivity", "Received saved cards: ${cards.size} cards")
        }
    }
    
    // Manual refresh function
    fun refreshSavedCards() {
        scope.launch {
            try {
                val cards = nfcManager.getSavedCards().first()
                savedCards = cards
                Log.d("MainActivity", "Manually refreshed saved cards: ${cards.size} cards")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error refreshing saved cards: ${e.message}")
            }
        }
    }
    

    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // NFC Status Section
            item {
                NFCStatusCard(nfcManager, nfcAdapter)
            }
            
            // Read NFC Section
            item {
                ReadNFCSection(
                    nfcManager = nfcManager
                )
            }
            
            // Current Card Section
            currentCard?.let { card ->
                Log.d("MainActivity", "Rendering CurrentCardSection for card: ${card.id}")
                item {
                    CurrentCardSection(
                        card = card,
                        onSave = {
                            Log.d("MainActivity", "Save button clicked for card: ${card.id}")
                            showSaveDialog = true
                            cardName = "Card_${System.currentTimeMillis()}"
                        }
                    )
                }
            } ?: run {
                Log.d("MainActivity", "No currentCard, not showing CurrentCardSection")
            }
            
            // Saved Cards Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Saved Cards (${savedCards.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { refreshSavedCards() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                Icons.Default.Refresh, 
                                contentDescription = "Refresh",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh", fontWeight = FontWeight.Medium)
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    nfcManager.debugFileContents()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                Icons.Default.BugReport, 
                                contentDescription = "Debug",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Debug", fontWeight = FontWeight.Medium)
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    val logs = logManager?.readLogs()
                                    val logInfo = logManager?.getLogFileInfo()
                                    Log.d(TAG, "=== SAVED LOGS ===")
                                    Log.d(TAG, "" +  logInfo)
                                    Log.d(TAG, "=== LOG CONTENTS ===")
                                    Log.d(TAG, "" + logs)
                                    Log.d(TAG, "=== END LOGS ===")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                Icons.Default.List, 
                                contentDescription = "Show Logs",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Show Logs", fontWeight = FontWeight.Medium)
                        }
                        
                        Button(
                            onClick = {
                                val status = NFCEmulationService.getEmulationStatus()
                                Log.d("MainActivity", "=== EMULATION STATUS ===")
                                Log.d("MainActivity", status)
                                Log.d("MainActivity", "=== END STATUS ===")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Info, 
                                contentDescription = "Status",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Status", fontWeight = FontWeight.Medium)
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    val logPath = logManager?.getLogFilePath()
                                    val logInfo = logManager?.getLogFileInfo()
                                    Log.d(TAG, "=== LOG FILE INFO ===")
                                    Log.d(TAG, "" + logInfo)
                                    Log.d(TAG, "Log file path: $logPath")
                                    Log.d(TAG, "=== END LOG FILE INFO ===")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Folder, 
                                contentDescription = "Log Path",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Path", fontWeight = FontWeight.Medium)
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    val cleared = logManager?.clearLogs()
                                    Log.d(TAG, "=== CLEAR LOGS ===")
                                    Log.d(TAG, "Logs cleared: $cleared")
                                    Log.d(TAG, "=== END CLEAR LOGS ===")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Clear, 
                                contentDescription = "Clear Logs",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Logs", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
                
            if (savedCards.isNotEmpty()) {
                items(savedCards) { savedCard ->
                    SavedCardItem(
                        card = savedCard,
                        onEmulate = {
                            scope.launch {
                                try {
                                    Log.d("MainActivity", "=== STARTING EMULATION ===")
                                    Log.d("MainActivity", "Card ID: ${savedCard.id}")
                                    Log.d("MainActivity", "Card Name: ${savedCard.name}")
                                    Log.d("MainActivity", "Card Technologies: ${savedCard.data.techList}")
                                    
                                    val cardData = nfcManager.getCardDataForEmulation(savedCard.id)
                                    if (cardData != null) {
                                        Log.d("MainActivity", "Card data retrieved successfully")
                                        Log.d("MainActivity", "Data size: ${cardData.data.values.sumOf { it.size }} bytes")
                                        
                                        NFCEmulationService.setEmulatedCard(savedCard.id, cardData)
                                        isEmulating = true
                                        emulatedCardId = savedCard.id
                                        
                                        Log.d("MainActivity", "Emulation started successfully")
                                        Log.d("MainActivity", "Emulated card ID: $emulatedCardId")
                                        Log.d("MainActivity", "Is emulating: $isEmulating")
                                        Log.d("MainActivity", "Emulation service started")
                                    } else {
                                        Log.e("MainActivity", "Failed to get card data for emulation")
                                        Log.e("MainActivity", "Card data is null")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error starting emulation: ${e.message}")
                                    e.printStackTrace()
                                }
                                Log.d("MainActivity", "=== END EMULATION START ===")
                            }
                        },
                        onStopEmulation = {
                            scope.launch {
                                try {
                                    Log.d("MainActivity", "=== STOPPING EMULATION ===")
                                    Log.d("MainActivity", "Card ID: ${savedCard.id}")
                                    Log.d("MainActivity", "Current emulated card ID: $emulatedCardId")
                                    Log.d("MainActivity", "Is emulating: $isEmulating")
                                    
                                    NFCEmulationService.setEmulatedCard(savedCard.id, null)
                                    isEmulating = false
                                    emulatedCardId = null
                                    
                                    Log.d("MainActivity", "Emulation stopped successfully")
                                    Log.d("MainActivity", "Emulated card ID: $emulatedCardId")
                                    Log.d("MainActivity", "Is emulating: $isEmulating")
                                    Log.d("MainActivity", "Emulation service stopped")
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error stopping emulation: ${e.message}")
                                    e.printStackTrace()
                                }
                                Log.d("MainActivity", "=== END EMULATION STOP ===")
                            }
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    nfcManager.removeCard(savedCard.id)
                                    Log.d("MainActivity", "Deleted card: ${savedCard.id}")
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error deleting card: ${e.message}")
                                }
                            }
                        },
                        isCurrentlyEmulating = emulatedCardId == savedCard.id
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No cards",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No saved NFC cards yet",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Read and save an NFC card to see it here",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Save Card Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Card") },
            text = {
                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Card Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Log.d("MainActivity", "Save button clicked in dialog, currentCard: ${currentCard?.id}, cardName: $cardName")
                        currentCard?.let { card ->
                            scope.launch {
                                try {
                                    Log.d("MainActivity", "Starting to save card: ${card.id}")
                                    val savedCard = nfcManager.saveCard(card, cardName)
                                    if (savedCard != null) {
                                        showSaveDialog = false
                                        Log.d("MainActivity", "Card saved successfully: ${savedCard.name}")
                                        // Refresh the saved cards list
                                        refreshSavedCards()
                                    } else {
                                        Log.e("MainActivity", "Failed to save card - returned null")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error saving card: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        } ?: run {
                            Log.e("MainActivity", "currentCard is null when trying to save")
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NFCStatusCard(nfcManager: NFCManager, nfcAdapter: NfcAdapter?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "NFC Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (nfcManager.isNfcAvailable()) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = "NFC Available",
                    tint = if (nfcManager.isNfcAvailable()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (nfcManager.isNfcAvailable()) "NFC Available" else "NFC Not Available",
                    color = if (nfcManager.isNfcAvailable()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            if (nfcManager.isNfcAvailable()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (nfcManager.isNfcEnabled()) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = "NFC Enabled",
                        tint = if (nfcManager.isNfcEnabled()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (nfcManager.isNfcEnabled()) "NFC Enabled" else "NFC Disabled",
                        color = if (nfcManager.isNfcEnabled()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ReadNFCSection(
    nfcManager: NFCManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Read NFC Card",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Place an NFC card near your device to read it",
                fontSize = 14.sp
            )
            
            Button(
                onClick = { /* NFC reading is automatic */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = nfcManager.isNfcAvailable() && nfcManager.isNfcEnabled()
            ) {
                Icon(Icons.Default.Nfc, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ready to Read NFC")
            }
        }
    }
}

@Composable
fun CurrentCardSection(
    card: NFCData,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
    Text(
                text = "Current Card",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text("ID: ${card.id}")
            Text("Technologies: ${card.techList.size}")
            Text("Data Size: ${card.data.values.sumOf { it.size }} bytes")
            
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Card")
            }
        }
    }
}

@Composable
fun SavedCardItem(
    card: NFCCard,
    onEmulate: () -> Unit,
    onStopEmulation: () -> Unit,
    onDelete: () -> Unit,
    isCurrentlyEmulating: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyEmulating) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentlyEmulating) 8.dp else 4.dp
        ),
        border = if (isCurrentlyEmulating) 
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with card info and emulation status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = card.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentlyEmulating) 
                            MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Card ID with copy icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC Card",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = card.id,
                            fontSize = 14.sp,
                            color = if (isCurrentlyEmulating) 
                                MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    
                    // Card details
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Technologies count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Technologies",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${card.data.techList.size} tech",
                                fontSize = 12.sp,
                                color = if (isCurrentlyEmulating) 
                                    MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Data size
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataUsage,
                                contentDescription = "Data Size",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${card.data.data.values.sumOf { it.size }} bytes",
                                fontSize = 12.sp,
                                color = if (isCurrentlyEmulating) 
                                    MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Emulation status badge
                if (isCurrentlyEmulating) {
                    Card(
                        modifier = Modifier.padding(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Emulating",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Emulating", 
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Technology list
            if (card.data.techList.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentlyEmulating) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (isCurrentlyEmulating) 
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Technologies",
                                tint = if (isCurrentlyEmulating) 
                                    MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Supported Technologies",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCurrentlyEmulating) 
                                    MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(card.data.techList) { tech ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentlyEmulating) 
                                            MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Text(
                                        text = tech.split(".").last(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isCurrentlyEmulating) 
                                            MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isCurrentlyEmulating) {
                    // Primary action when emulating
                    Button(
                        onClick = onStopEmulation,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop, 
                            contentDescription = "Stop Emulation",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Stop Emulation", 
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    // Secondary actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = "Delete Card",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Card", fontWeight = FontWeight.Medium)
                        }
                        
                        // Add info about emulation
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Card is being emulated",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // Primary action when not emulating
                    Button(
                        onClick = onEmulate,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow, 
                            contentDescription = "Start Emulation",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Start Emulation", 
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    // Secondary action
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete Card",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Card", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}