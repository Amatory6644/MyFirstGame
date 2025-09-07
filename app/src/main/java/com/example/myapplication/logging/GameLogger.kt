package com.example.myapplication.logging

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@Serializable
data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String,
    val playerId: String? = null,
    val action: String? = null,
    val data: String? = null
)

class GameLogger(private val context: Context) {
    
    companion object {
        private const val TAG = "GameLogger"
        private const val LOG_FILE_PREFIX = "game_log_"
        private const val MAX_LOG_ENTRIES = 1000
        private const val MAX_LOG_FILES = 5
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val json = Json { prettyPrint = false }
    
    // Логирование для античита
    fun logPlayerAction(
        playerId: String,
        action: String,
        data: String? = null,
        level: String = "INFO"
    ) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            tag = "ANTI_CHEAT",
            message = "Player action: $action",
            playerId = playerId,
            action = action,
            data = data
        )
        
        logQueue.offer(entry)
        writeToFile(entry)
        
        // Проверяем на аномалии
        checkForAnomalies(playerId, action, data)
    }
    
    // Обычное логирование
    fun log(level: String, tag: String, message: String, playerId: String? = null) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message,
            playerId = playerId
        )
        
        logQueue.offer(entry)
        writeToFile(entry)
        
        // Также логируем в Android Log
        when (level) {
            "ERROR" -> Log.e(tag, message)
            "WARN" -> Log.w(tag, message)
            "DEBUG" -> Log.d(tag, message)
            else -> Log.i(tag, message)
        }
    }
    
    private fun writeToFile(entry: LogEntry) {
        try {
            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val today = fileDateFormat.format(Date())
            val logFile = File(logDir, "$LOG_FILE_PREFIX$today.json")
            
            // Добавляем запись в файл
            val logLine = json.encodeToString(entry) + "\n"
            logFile.appendText(logLine)
            
            // Очищаем старые файлы
            cleanupOldLogFiles(logDir)
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка записи в лог файл: ${e.message}", e)
        }
    }
    
    private fun cleanupOldLogFiles(logDir: File) {
        try {
            val logFiles = logDir.listFiles { file ->
                file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }
            
            logFiles?.let { files ->
                if (files.size > MAX_LOG_FILES) {
                    files.drop(MAX_LOG_FILES).forEach { file ->
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка очистки старых лог файлов: ${e.message}", e)
        }
    }
    
    // Проверка на аномалии для античита
    private fun checkForAnomalies(playerId: String, action: String, data: String?) {
        when (action) {
            "MOVE" -> checkMovementAnomalies(playerId, data)
            "SHOOT" -> checkShootingAnomalies(playerId, data)
            "CONNECT" -> checkConnectionAnomalies(playerId, data)
        }
    }
    
    private fun checkMovementAnomalies(playerId: String, data: String?) {
        // Проверяем на слишком быстрые движения
        // В реальном приложении здесь будет более сложная логика
        log("WARN", "ANTI_CHEAT", "Проверка движения игрока: $playerId", playerId)
    }
    
    private fun checkShootingAnomalies(playerId: String, data: String?) {
        // Проверяем на слишком частую стрельбу
        log("WARN", "ANTI_CHEAT", "Проверка стрельбы игрока: $playerId", playerId)
    }
    
    private fun checkConnectionAnomalies(playerId: String, data: String?) {
        // Проверяем на подозрительные подключения
        log("WARN", "ANTI_CHEAT", "Проверка подключения игрока: $playerId", playerId)
    }
    
    // Получение логов для отправки на сервер
    fun getLogsForServer(): List<LogEntry> {
        return logQueue.toList()
    }
    
    // Очистка очереди после отправки
    fun clearSentLogs() {
        logQueue.clear()
    }
    
    // Получение логов из файла
    fun getLogsFromFile(date: String): List<LogEntry> {
        return try {
            val logDir = File(context.filesDir, "logs")
            val logFile = File(logDir, "$LOG_FILE_PREFIX$date.json")
            
            if (logFile.exists()) {
                logFile.readLines().mapNotNull { line ->
                    try {
                        json.decodeFromString<LogEntry>(line.trim())
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения лог файла: ${e.message}", e)
            emptyList()
        }
    }
}
