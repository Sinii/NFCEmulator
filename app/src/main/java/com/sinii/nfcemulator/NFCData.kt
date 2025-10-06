@file:Suppress("DEPRECATION", "unused")

package com.sinii.nfcemulator

import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Parcel
import android.os.Parcelable

data class NFCData(
    val id: String,
    val techList: List<String>,
    val data: Map<String, ByteArray>,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        techList = parcel.createStringArrayList() ?: emptyList(),
        data = mutableMapOf<String, ByteArray>().apply {
            val keys = parcel.createStringArrayList() ?: emptyList()
            val values = mutableListOf<ByteArray>()
            parcel.readList(values, ByteArray::class.java.classLoader)
            keys.forEachIndexed { index, key ->
                if (index < values.size) {
                    put(key, values[index])
                }
            }
        },
        timestamp = parcel.readLong()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeStringList(techList)
        parcel.writeStringList(data.keys.toList())
        parcel.writeList(data.values.toList())
        parcel.writeLong(timestamp)
    }
    
    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<NFCData> {
            override fun createFromParcel(parcel: Parcel): NFCData {
                return NFCData(parcel)
            }

            override fun newArray(size: Int): Array<NFCData?> {
                return arrayOfNulls(size)
            }
        }

        fun byteArrayToHex(bytes: ByteArray): String {
            return bytes.joinToString("") { "%02x".format(it) }
        }

    }
}

data class NFCCard(
    val id: String,
    val name: String,
    val data: NFCData,
    val isEmulating: Boolean = false
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        name = parcel.readString() ?: "",
        data = parcel.readParcelable(NFCData::class.java.classLoader) ?: NFCData("", emptyList(), emptyMap()),
        isEmulating = parcel.readInt() == 1
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeParcelable(data, flags)
        parcel.writeInt(if (isEmulating) 1 else 0)
    }
    
    override fun describeContents(): Int {
        return 0
    }
    
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<NFCCard> {
            override fun createFromParcel(parcel: Parcel): NFCCard {
                return NFCCard(parcel)
            }
            
            override fun newArray(size: Int): Array<NFCCard?> {
                return arrayOfNulls(size)
            }
        }
    }
}

enum class NFCTechType {
    MIFARE_CLASSIC,
    MIFARE_ULTRALIGHT,
    NDEF,
    ISO_DEP,
    NFC_A,
    NFC_B,
    NFC_F,
    NFC_V,
    UNKNOWN
}

object NFCTechHelper {
    fun getTechType(tech: String): NFCTechType {
        return when (tech) {
            MifareClassic::class.java.name -> NFCTechType.MIFARE_CLASSIC
            MifareUltralight::class.java.name -> NFCTechType.MIFARE_ULTRALIGHT
            Ndef::class.java.name -> NFCTechType.NDEF
            IsoDep::class.java.name -> NFCTechType.ISO_DEP
            NfcA::class.java.name -> NFCTechType.NFC_A
            NfcB::class.java.name -> NFCTechType.NFC_B
            NfcF::class.java.name -> NFCTechType.NFC_F
            NfcV::class.java.name -> NFCTechType.NFC_V
            else -> NFCTechType.UNKNOWN
        }
    }
    
    fun getTechDisplayName(tech: String): String {
        return when (getTechType(tech)) {
            NFCTechType.MIFARE_CLASSIC -> "Mifare Classic"
            NFCTechType.MIFARE_ULTRALIGHT -> "Mifare Ultralight"
            NFCTechType.NDEF -> "NDEF"
            NFCTechType.ISO_DEP -> "ISO-DEP"
            NFCTechType.NFC_A -> "NFC-A"
            NFCTechType.NFC_B -> "NFC-B"
            NFCTechType.NFC_F -> "NFC-F"
            NFCTechType.NFC_V -> "NFC-V"
            NFCTechType.UNKNOWN -> "Unknown"
        }
    }
}
