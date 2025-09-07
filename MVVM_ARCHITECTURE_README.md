# MVVM Архитектура для MyFirstGame

## Обзор

Проект был реструктурирован согласно принципам MVVM (Model-View-ViewModel) архитектуры с использованием современных Android технологий:

- **Kotlin Coroutines** для асинхронных операций
- **Flow** для реактивного программирования
- **Koin** для внедрения зависимостей
- **Ktor** для HTTP и WebSocket соединений

## Структура проекта

```
app/src/main/java/com/example/myapplication/
├── di/                          # Внедрение зависимостей
│   └── AppModule.kt            # Koin модуль
├── network/                     # Сетевой слой
│   └── NetworkManager.kt       # Управление WebSocket
├── repository/                  # Слой данных
│   └── GameRepository.kt       # Сетевые операции
├── LoginViewModel.kt            # ViewModel для входа
├── PlayerViewModel.kt           # ViewModel для игрока
├── MyApplication.kt             # Application класс
└── ...                         # Остальные файлы
```

## Основные компоненты

### 1. GameRepository
Центральный класс для сетевых операций:
- HTTP запросы (аутентификация, приглашения)
- WebSocket соединения
- Обработка входящих сообщений
- Использование SharedFlow для передачи данных

### 2. NetworkManager
Управляет WebSocket соединением:
- Установка/разрыв соединения
- Отправка обновлений позиции игрока
- Управление жизненным циклом соединения

### 3. ViewModels
- **LoginViewModel**: Аутентификация пользователя
- **PlayerViewModel**: Управление игроком и игровым состоянием

## Как использовать

### В Activity/Fragment:

```kotlin
// Получение ViewModel через Koin
private val loginViewModel: LoginViewModel by viewModel()
private val playerViewModel: PlayerViewModel by viewModel()

// Наблюдение за состоянием
lifecycleScope.launch {
    loginViewModel.state.collect { state ->
        when (state) {
            is State.Loading -> showLoading()
            is State.Success -> navigateToGame()
            is State.Error -> showError(state.message)
        }
    }
}

// Вызов методов
loginViewModel.checkLogin(login, password)
playerViewModel.connectToGame("/game")
```

### Обновление позиции игрока:

```kotlin
// В GameView или другом UI компоненте
playerViewModel.updatePosition(dx, dy)

// Наблюдение за позицией
lifecycleScope.launch {
    playerViewModel.playerData.collect { playerData ->
        // Обновление UI
        updatePlayerPosition(playerData.x, playerData.y)
    }
}
```

## Преимущества новой архитектуры

1. **Разделение ответственности**: Каждый класс имеет четкую роль
2. **Тестируемость**: Легко создавать unit тесты с Mock объектами
3. **Реактивность**: Использование Flow для автоматического обновления UI
4. **Жизненный цикл**: Автоматическое управление ресурсами
5. **Масштабируемость**: Легко добавлять новые функции

## Миграция со старого кода

### Старый способ:
```kotlin
// Прямое создание HttpClient
val client = HttpClientProvider.create()
// Прямое управление WebSocket
val webSocket = WebSoketManager()
```

### Новый способ:
```kotlin
// Внедрение через Koin
class MyActivity : AppCompatActivity() {
    private val playerViewModel: PlayerViewModel by viewModel()
    
    // Автоматическое управление зависимостями
}
```

## Тестирование

Для unit тестов используйте `HttpClientProvider.testModule`:

```kotlin
@Before
fun setup() {
    startKoin {
        modules(HttpClientProvider.testModule)
    }
}
```

## Следующие шаги

1. Обновить существующие Activity для использования новых ViewModel
2. Добавить обработку ошибок сети
3. Реализовать кэширование данных
4. Добавить логирование и аналитику
5. Создать unit тесты для ViewModel и Repository
