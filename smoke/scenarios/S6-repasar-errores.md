# S6 · Repasar errores (tarjetas solo con errores)

**Цель:** пункт «Repasar errores» в списке тем неактивен без ошибок, а после ошибок строит короткую сессию только из них (#21).
**Предусловие:** чистое состояние.

```steps
clear
launch
tap: Comenzar tarjetas didácticas
expect: Repasar errores
expect: Sin errores todavía
tap: Repasar errores
expect: Temas de Tarjetas
expect_absent: Tarjeta 1 de
tap: Todos los temas
expect: Tarjeta 1 de 10
repeat 3:
  tap: Toca para ver la respuesta
  expect: Otra vez
  tap: Otra vez
expect: Tarjeta 4 de 10
tap: Cerrar tarjetas
expect: Temas de Tarjetas
expect_absent: Sin errores todavía
tap: Repasar errores
expect: Tarjeta 1 de 3
tap: Cerrar tarjetas
back
expect: Comenzar quiz
tap: Comenzar quiz
expect: Temas del Quiz
tap: Repasar errores
expect: Pregunta 1 de 3
```

**Ожидание:** на чистой статистике «Repasar errores» подписан «Sin errores todavía», тап по нему ничего не открывает; после трёх оценок «Otra vez» пункт активен (3 preguntas) и в карточках, и в квизе, сессия — 3 карты / 3 вопроса вместо 10.
