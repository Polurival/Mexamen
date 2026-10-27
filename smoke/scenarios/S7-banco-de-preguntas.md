# S7 · Banco de preguntas: lista, explicación y búsqueda

**Цель:** открыть весь гид с Home, раскрыть explicación тапом, найти вопрос без учёта регистра и диакритики (#22).
**Предусловие:** чистое состояние.

```steps
clear
launch
tap: Banco de preguntas
expect: Buscar
expect: Preguntas: 776 de 776
expect: ¿Cuál es la capital de Aguascalientes?
expect_absent: Su nombre se debe a las aguas termales
tap: ¿Cuál es la capital de Aguascalientes?
expect: Explicación
expect: Su nombre se debe a las aguas termales
tap: ¿Cuál es la capital de Aguascalientes?
expect_absent: Su nombre se debe a las aguas termales
tap: Buscar
type: canon
expect: Preguntas: 2 de 776
expect: Cañón del Sumidero
expect: Chiapas
expect: Coro del himno mexicano
tap: Volver
expect: Comenzar quiz
tap: Banco de preguntas
expect: Preguntas: 776 de 776
tap: Buscar
type: SUMIDERO
expect: Preguntas: 1 de 776
expect: Cañón del Sumidero
expect_absent: Coro del himno mexicano
tap: Volver
expect: Comenzar quiz
```

**Ожидание:** кнопка «Banco de preguntas» в карточке «Historia y cultura» открывает список из 776 вопросов с ответами; тап по карточке раскрывает и сворачивает «Explicación»; запрос «canon» (без тильды и акцента) находит «Cañón del Sumidero» и гимн («…rugir del cañón» в ответе); «SUMIDERO» заглавными — ровно 1 вопрос; после выхода с экрана запрос сбрасывается.
