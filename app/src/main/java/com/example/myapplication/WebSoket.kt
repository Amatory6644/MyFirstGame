package com.example.myapplication

// Этот файл больше не используется в новой MVVM архитектуре
// WebSocket функциональность теперь реализована в:
// - GameRepository.kt - для обработки входящих сообщений
// - NetworkManager.kt - для управления соединением
// - PlayerViewModel.kt - для интеграции с UI

// Старая реализация WebSoketManager заменена на:
// 1. GameRepository.connectToGame() - для установки WebSocket соединения
// 2. NetworkManager.connectToGame() - для управления жизненным циклом соединения
// 3. SharedFlow в Repository для передачи данных к ViewModel

// Для использования новой архитектуры:
// - В LoginActivity используйте LoginViewModel
// - В GameView используйте PlayerViewModel
// - Все сетевые операции выполняются через Repository