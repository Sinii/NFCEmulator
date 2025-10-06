package com.sinii.nfcemulator

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class LogManager(private val context: Context) {
    
    companion object {
        const val TAG = "LogManager"
        private const val LOG_FILE_NAME = "nfcemulator_logs.txt"
        private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB max log file size
    }
    
    private val logFile: File = File(context.getExternalFilesDir(null), LOG_FILE_NAME)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Save a log message to the log file with timestamp
     */
    fun saveLog(tag: String, level: String, message: String) {
        try {
            val timestamp = dateFormat.format(Date())
            val logEntry = "[$timestamp] $level/$tag: $message\n"
            
            // Check if file exists and rotate if too large
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile()
            }
            
            FileWriter(logFile, true).use { writer ->
                writer.write(logEntry)
                writer.flush()
            }
            
            Log.d(TAG, "Log saved: $tag/$level: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving log: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Read all logs from the file
     */
    fun readLogs(): String {
        return try {
            if (logFile.exists()) {
                val content = logFile.readText()
                Log.d(TAG, "Read ${content.length} characters from log file")
                content
            } else {
                "No log file found at: ${logFile.absolutePath}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading logs: ${e.message}")
            "Error reading logs: ${e.message}"
        }
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs(): Boolean {
        return try {
            if (logFile.exists()) {
                logFile.delete()
                Log.d(TAG, "Log file cleared")
                true
            } else {
                Log.d(TAG, "No log file to clear")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logs: ${e.message}")
            false
        }
    }
    
    /**
     * Get log file info
     */
    fun getLogFileInfo(): String {
        return try {
            if (logFile.exists()) {
                val size = logFile.length()
                val lastModified = dateFormat.format(Date(logFile.lastModified()))
                "Log file: ${logFile.absolutePath}\nSize: ${size} bytes\nLast modified: $lastModified"
            } else {
                "Log file does not exist"
            }
        } catch (e: Exception) {
            "Error getting log file info: ${e.message}"
        }
    }
    
    /**
     * Rotate log file by creating a backup
     */
    private fun rotateLogFile() {
        try {
            val backupFile = File(context.getExternalFilesDir(null), "nfcemulator_logs_backup.txt")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            logFile.renameTo(backupFile)
            Log.d(TAG, "Log file rotated to backup")
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating log file: ${e.message}")
        }
    }
    
    /**
     * Get log file path for external access
     */
    fun getLogFilePath(): String {
        return logFile.absolutePath
    }
}





