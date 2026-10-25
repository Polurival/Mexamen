# S6 · Banco de preguntas: lista y explicación

**Цель:** открыть банк вопросов с главной, увидеть пары «вопрос — ответ», раскрыть explicación тапом и вернуться.
**Предусловие:** чистое состояние (статистика не нужна, сеть не нужна).

```steps
clear
launch
tap: Banco de preguntas
expect: Buscar por pregunta o respuesta
expect: ¿Cuál es la capital de Aguascalientes?
expect_absent: aguas termales
tap: ¿Cuál es la capital de Aguascalientes?
expect: aguas termales
back
expect: Comenzar quiz
```

**Ожидание:** экран открывается с полем поиска и полным списком; тап по карточке показывает объяснение; системная «назад» ведёт на главную. Поиск (ввод текста) словарём шагов не покрыт — проверяется тестами `QuestionSearchTest`/`QuestionBankViewModelTest`.
