---
name: Mexamen rules (compact, для 1.5–3B моделей на CPU)
alwaysApply: true
---

Проект Mexamen: Kotlin Multiplatform + Compose Multiplatform + Material3. Таргеты Android и wasmJs.

Жёсткие правила:
1. Тексты UI — только испанские литералы в composable. НЕТ `stringResource`, НЕТ `R.string`.
2. Иконки — только `AppIcons.X` из `ui/AppIcons.kt`. НЕТ `Icons.Default`, `material-icons` не подключён.
3. В `commonMain` НЕТ `android.*`, `java.*`, `LocalContext`, `System.currentTimeMillis`.
4. Screen — stateless: `fun XxxScreen(state, onAction: () -> Unit, modifier: Modifier = Modifier)`. Логика и агрегация — в `domain/`, не в composable.
5. ViewModel: приватный `_state: MutableStateFlow`, публичный `state: StateFlow` через `asStateFlow()`.
6. Один публичный тип на файл. Не добавляй зависимости. Не меняй публичные сигнатуры и код вне задачи.
7. Отступы: 24.dp у экрана, 16.dp внутри Card. Цвета и шрифты — из `MaterialTheme`.

Как работать:
- Сначала ПРОЧИТАЙ файл, потом правь. Меняй минимально, остальной код оставь как есть.
- Проверка: `./gradlew :composeApp:compileAndroidMain --console=plain`.
- Не пиши «готово», если не видел успешную сборку.
