package com.sinii.nfcemulator

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinii.nfcemulator.ui.theme.NFCEmulatorTheme

@Suppress("DEPRECATION")
class NFCReaderActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "NFCReaderActivity"
        const val EXTRA_NFC_DATA = "nfc_data"
    }
    
    private lateinit var nfcManager: NFCManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        nfcManager = NFCManager(this)
        
        // Check if we have an NFC intent
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            Log.d(TAG, "Tag received in onCreate: ${tag.id.contentToString()}")
        }
        
        setContent {
            NFCEmulatorTheme {
                NFCReaderScreen(
                    nfcManager = nfcManager,
                    initialTag = tag,
                    onBackToMain = { nfcData ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_NFC_DATA, nfcData)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            Log.d(TAG, "New tag received: ${tag.id.contentToString()}")
        }
    }
}

@Composable
fun NFCReaderScreen(
    nfcManager: NFCManager,
    initialTag: Tag?,
    onBackToMain: (NFCData) -> Unit
) {
    var nfcData by remember { mutableStateOf<NFCData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    
    // Handle NFC tag reading when the screen is first displayed
    LaunchedEffect(initialTag) {
        initialTag?.let { tag ->
            Log.d("NFCReaderScreen", "Starting to read tag: ${tag.id.contentToString()}")
            try {
                nfcData = nfcManager.readNFCTag(tag)
                if (nfcData != null) {
                    Log.d("NFCReaderScreen", "Successfully read NFC tag: ${nfcData!!.id}")
                } else {
                    Log.e("NFCReaderScreen", "Failed to read NFC tag")
                    error = "Failed to read NFC tag"
                }
            } catch (e: Exception) {
                Log.e("NFCReaderScreen", "Error reading NFC tag: ${e.message}")
                error = "Error reading NFC tag: ${e.message}"
            } finally {
                isLoading = false
            }
        } ?: run {
            Log.w("NFCReaderScreen", "No tag provided")
            error = "No NFC tag provided"
            isLoading = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Reading NFC tag...", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please wait while we read the tag data", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            error != null -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Error", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Text(error!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { (context as? Activity)?.finish() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go Back")
                }
            }
            nfcData != null -> {
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
                            text = "NFC Tag Read Successfully!",
                            fontSize = 20.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        
                        Text("Tag ID: ${nfcData!!.id}")
                        Text("Technologies: ${nfcData!!.techList.size}")
                        Text("Data Size: ${nfcData!!.data.values.sumOf { it.size }} bytes")
                        
                        Button(
                            onClick = { onBackToMain(nfcData!!) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue to Main App")
                        }
                    }
                }
            }
        }
    }
}
