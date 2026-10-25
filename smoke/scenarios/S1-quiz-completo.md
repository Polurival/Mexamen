# S1 · Quiz completo por todos los temas

**Цель:** пройти квиз из 10 вопросов оффлайн и увидеть экран результата.
**Предусловие:** чистое состояние приложения (`clear`), сеть не нужна.

```steps
clear
launch
expect: Comenzar quiz
tap: Comenzar quiz
expect: Todos los temas
expect: Sin errores todavía
tap: Todos los temas
expect: Pregunta 1 de 10
repeat 10:
  tap_option: 1
  expect: Siguiente
  tap: Siguiente
expect: Resultado:
expect: Volver al inicio
tap: Volver al inicio
expect: Temas del
back
expect: Prepárate para el examen de naturalización
```

**Ожидание:** на чистом состоянии пункт «Repasar errores» неактивен с подписью «Sin errores todavía»; 10 вопросов подряд, после ответа показан блок «Explicación» и кнопка «Siguiente»; в конце «Resultado: N/10»; «Volver al inicio» закрывает сессию и возвращает к списку тем (штатное поведение), «назад» — на главную.
